package br.pucrs.pulsepass.ingressos.aplicacao;

import br.pucrs.pulsepass.commons.eventos.Uuids;
import br.pucrs.pulsepass.commons.outbox.OutboxWriter;
import br.pucrs.pulsepass.ingressos.dominio.AssinaturaQr;
import br.pucrs.pulsepass.ingressos.dominio.CodigoQr;
import br.pucrs.pulsepass.ingressos.dominio.ResultadoValidacao;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ValidarIngresso (US29): assinatura + uso único por UPDATE condicional (invariante I9). */
@Service
public class ValidacaoService {

  private final JdbcTemplate jdbc;
  private final AssinaturaQr assinatura;
  private final OutboxWriter outbox;

  public ValidacaoService(JdbcTemplate jdbc, AssinaturaQr assinatura, OutboxWriter outbox) {
    this.jdbc = jdbc;
    this.assinatura = assinatura;
    this.outbox = outbox;
  }

  public record Resultado(ResultadoValidacao resultado, UUID ingressoId, String setorAssento, String titular) {
  }

  @Transactional
  public Resultado validar(String textoQr, UUID operadorId) {
    Optional<CodigoQr> lido = CodigoQr.ler(textoQr);
    if (lido.isEmpty()) {
      return new Resultado(ResultadoValidacao.INVALIDO, null, null, null);
    }
    CodigoQr qr = lido.get();
    boolean assinaturaOk = assinatura.verificar(qr.mensagemAssinada(), qr.assinatura());
    Map<String, Object> ingresso = jdbc.query("SELECT setor_assento, titular_nome FROM ingresso WHERE id = ? AND evento_id = ?",
        rs -> rs.next() ? Map.<String, Object>of("assento", rs.getString(1), "titular", rs.getString(2)) : null,
        qr.ingressoId(), qr.eventoId());
    boolean marcou = false;
    if (assinaturaOk && ingresso != null) {
      marcou = jdbc.update("""
          UPDATE ingresso SET estado = 'UTILIZADO', utilizado_em = now() WHERE id = ? AND estado = 'EMITIDO'
          """, qr.ingressoId()) == 1;
    }
    ResultadoValidacao r = ResultadoValidacao.decidir(assinaturaOk, ingresso != null, marcou);
    if (ingresso != null) {
      jdbc.update("INSERT INTO validacao (id, ingresso_id, operador_id, resultado, lido_em) VALUES (?, ?, ?, ?, now())",
          Uuids.v7(), qr.ingressoId(), operadorId, r.name());
    }
    if (r == ResultadoValidacao.LIBERADO) {
      outbox.gravar(PdfJobWorker.TOPICO_INGRESSOS, "IngressoUtilizado", qr.ingressoId(), 2, qr.ingressoId(),
          qr.ingressoId(), Map.of("ingressoId", qr.ingressoId(), "operadorId", operadorId));
    }
    return new Resultado(r, ingresso == null ? null : qr.ingressoId(),
        ingresso == null ? null : (String) ingresso.get("assento"),
        ingresso == null ? null : (String) ingresso.get("titular"));
  }
}
