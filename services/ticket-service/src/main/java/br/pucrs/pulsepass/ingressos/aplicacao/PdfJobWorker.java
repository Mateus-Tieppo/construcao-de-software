package br.pucrs.pulsepass.ingressos.aplicacao;

import br.pucrs.pulsepass.commons.outbox.OutboxWriter;
import br.pucrs.pulsepass.ingressos.adaptadores.pdf.CatalogoClient;
import br.pucrs.pulsepass.ingressos.adaptadores.pdf.GeradorPdf;
import br.pucrs.pulsepass.ingressos.dominio.AssinaturaQr;
import br.pucrs.pulsepass.ingressos.dominio.CodigoQr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera PDF + QR assinado de forma assíncrona e repetível (Pacote 2, seção 3.7).
 * Mesmo ingressoId sobrescreve o mesmo arquivo (gravação em temporário + rename atômico); 3 tentativas.
 */
@Component
public class PdfJobWorker {

  public static final String TOPICO_INGRESSOS = "ingressos.v1";
  private static final Logger LOG = LoggerFactory.getLogger(PdfJobWorker.class);
  private static final int MAX_TENTATIVAS = 3;

  private final JdbcTemplate jdbc;
  private final GeradorPdf gerador;
  private final AssinaturaQr assinatura;
  private final OutboxWriter outbox;
  private final CatalogoClient catalogo;
  private final Path diretorio;

  public PdfJobWorker(JdbcTemplate jdbc, GeradorPdf gerador, AssinaturaQr assinatura, OutboxWriter outbox,
      CatalogoClient catalogo, @Value("${pulsepass.pdf.diretorio:/tmp/pulsepass-ingressos}") String diretorio) {
    this.catalogo = catalogo;
    this.jdbc = jdbc;
    this.gerador = gerador;
    this.assinatura = assinatura;
    this.outbox = outbox;
    this.diretorio = Path.of(diretorio);
  }

  record Job(UUID ingressoId, UUID eventoId, String setorAssento, String titular) {
  }

  @Scheduled(fixedDelayString = "${pulsepass.pdf.intervalo-ms:1000}")
  @Transactional
  public int processar() {
    List<Job> jobs = jdbc.query("""
        SELECT j.ingresso_id, i.evento_id, i.setor_assento, i.titular_nome
          FROM job_pdf j JOIN ingresso i ON i.id = j.ingresso_id
         WHERE j.estado = 'PENDENTE' ORDER BY j.ingresso_id LIMIT 10 FOR UPDATE OF j SKIP LOCKED
        """, (rs, n) -> new Job(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
        rs.getString(4)));
    int emitidos = 0;
    for (Job job : jobs) {
      try {
        emitir(job);
        emitidos++;
      } catch (RuntimeException | IOException e) {
        jdbc.update("""
            UPDATE job_pdf SET tentativas = tentativas + 1, ultimo_erro = ?,
                   estado = CASE WHEN tentativas + 1 >= ? THEN 'FALHOU' ELSE 'PENDENTE' END
             WHERE ingresso_id = ?
            """, e.getMessage(), MAX_TENTATIVAS, job.ingressoId());
        LOG.warn("PDF do ingresso {} falhou: {}", job.ingressoId(), e.getMessage());
      }
    }
    return emitidos;
  }

  private void emitir(Job job) throws IOException {
    Instant emitidoEm = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    String assinado = assinatura.assinar(CodigoQr.mensagem(job.ingressoId(), job.eventoId(), emitidoEm));
    CodigoQr qr = new CodigoQr(job.ingressoId(), job.eventoId(), emitidoEm, assinado);
    String nomeEvento = catalogo.nomeEvento(job.eventoId());
    byte[] pdf = gerador.gerar(new GeradorPdf.DadosIngresso(job.ingressoId().toString(),
        nomeEvento == null ? "Evento " + job.eventoId() : nomeEvento, job.setorAssento(), job.titular(), emitidoEm,
        qr.texto()));
    Files.createDirectories(diretorio);
    Path destino = diretorio.resolve(job.ingressoId() + ".pdf");
    Path temporario = diretorio.resolve(job.ingressoId() + ".pdf.tmp");
    Files.write(temporario, pdf);
    Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

    jdbc.update("""
        UPDATE ingresso SET estado = 'EMITIDO', emitido_em = ?, assinatura_qr = ?, codigo_qr = ?, caminho_pdf = ?
         WHERE id = ? AND estado = 'PENDENTE'
        """, Timestamp.from(emitidoEm), assinado, qr.texto(), destino.toString(), job.ingressoId());
    jdbc.update("UPDATE job_pdf SET estado = 'CONCLUIDO' WHERE ingresso_id = ?", job.ingressoId());
    Map<String, Object> dados = new LinkedHashMap<>();
    dados.put("ingressoId", job.ingressoId());
    dados.put("eventoId", job.eventoId());
    dados.put("emitidoEm", emitidoEm);
    outbox.gravar(TOPICO_INGRESSOS, "IngressoEmitido", job.ingressoId(), 1, job.ingressoId(), job.ingressoId(), dados);
  }
}
