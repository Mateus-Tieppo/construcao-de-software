package br.pucrs.pulsepass.checkout.adaptadores.persistencia;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Registros da Idempotency-Key (tabela idempotencia, retenção de 24 h). */
@Repository
public class IdempotenciaRepositorio {

  private final JdbcTemplate jdbc;

  public IdempotenciaRepositorio(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Registro(String hash, boolean concluido, Integer status, String contentType, byte[] corpo) {
  }

  /** Tenta reservar a chave; false se já existia. */
  public boolean registrar(String usuario, String escopo, String chave, String hash) {
    return jdbc.update("""
        INSERT INTO idempotencia (usuario_id, escopo, chave, hash_requisicao, estado, criado_em, expira_em)
        VALUES (?, ?, ?, ?, 'EM_ANDAMENTO', now(), now() + interval '24 hours')
        ON CONFLICT DO NOTHING
        """, usuario, escopo, chave, hash) == 1;
  }

  public Optional<Registro> buscar(String usuario, String escopo, String chave) {
    return jdbc.query("""
        SELECT hash_requisicao, estado, status_http, content_type, corpo_resposta FROM idempotencia
         WHERE usuario_id = ? AND escopo = ? AND chave = ? AND expira_em > now()
        """, (rs, n) -> new Registro(rs.getString(1), "CONCLUIDO".equals(rs.getString(2)),
        (Integer) rs.getObject(3), rs.getString(4), rs.getBytes(5)), usuario, escopo, chave).stream().findFirst();
  }

  public void concluir(String usuario, String escopo, String chave, int status, String contentType, byte[] corpo) {
    jdbc.update("""
        UPDATE idempotencia SET estado = 'CONCLUIDO', status_http = ?, content_type = ?, corpo_resposta = ?
         WHERE usuario_id = ? AND escopo = ? AND chave = ?
        """, status, contentType, corpo, usuario, escopo, chave);
  }

  /** Erros 5xx não ficam gravados: o cliente pode tentar de novo com a mesma chave. */
  public void descartar(String usuario, String escopo, String chave) {
    jdbc.update("DELETE FROM idempotencia WHERE usuario_id = ? AND escopo = ? AND chave = ?", usuario, escopo, chave);
  }

  public int limparVencidos() {
    return jdbc.update("DELETE FROM idempotencia WHERE expira_em <= now()");
  }
}
