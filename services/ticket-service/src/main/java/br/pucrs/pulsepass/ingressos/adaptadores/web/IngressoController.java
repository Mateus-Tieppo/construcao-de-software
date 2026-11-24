package br.pucrs.pulsepass.ingressos.adaptadores.web;

import br.pucrs.pulsepass.ingressos.aplicacao.ValidacaoService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Meus ingressos e download do PDF (US26); validação de QR pelo operador (US29). */
@RestController
public class IngressoController {

  private final JdbcTemplate jdbc;
  private final ValidacaoService validacao;

  public IngressoController(JdbcTemplate jdbc, ValidacaoService validacao) {
    this.jdbc = jdbc;
    this.validacao = validacao;
  }

  public record IngressoView(UUID id, UUID pedidoId, UUID eventoId, String setorAssento, String estado,
      Instant emitidoEm, Instant utilizadoEm, String codigoQr) {
  }

  public record LeituraQr(String codigoQr) {
  }

  @GetMapping("/ingressos")
  public List<IngressoView> meus(@AuthenticationPrincipal Jwt jwt) {
    return jdbc.query("""
        SELECT id, pedido_id, evento_id, setor_assento, estado, emitido_em, utilizado_em, codigo_qr
          FROM ingresso WHERE titular_id = ? ORDER BY setor_assento
        """, (rs, n) -> new IngressoView(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
        rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5),
        rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toInstant(),
        rs.getTimestamp(7) == null ? null : rs.getTimestamp(7).toInstant(), rs.getString(8)),
        UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/ingressos/{id}/pdf")
  public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) throws IOException {
    Map<String, Object> ingresso = jdbc.query("SELECT titular_id, caminho_pdf FROM ingresso WHERE id = ?",
        rs -> rs.next() ? Map.<String, Object>of("titular", rs.getObject(1, UUID.class),
            "caminho", rs.getString(2) == null ? "" : rs.getString(2)) : null, id);
    if (ingresso == null) {
      return ResponseEntity.notFound().build();
    }
    if (!ingresso.get("titular").equals(UUID.fromString(jwt.getSubject()))) {
      // US26: ingresso de outro usuário
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String caminho = (String) ingresso.get("caminho");
    if (caminho.isEmpty() || !Files.exists(Path.of(caminho))) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ingresso-" + id + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(Files.readAllBytes(Path.of(caminho)));
  }

  @PostMapping("/validacoes")
  public ValidacaoService.Resultado validar(@AuthenticationPrincipal Jwt jwt, @RequestBody LeituraQr leitura) {
    return validacao.validar(leitura.codigoQr(), UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/ingressos/estatisticas")
  public Map<String, Object> estatisticas() {
    return jdbc.queryForMap("""
        SELECT count(*) FILTER (WHERE estado = 'PENDENTE') AS pendentes,
               count(*) FILTER (WHERE estado = 'EMITIDO') AS emitidos,
               count(*) FILTER (WHERE estado = 'UTILIZADO') AS utilizados
          FROM ingresso
        """);
  }
}
