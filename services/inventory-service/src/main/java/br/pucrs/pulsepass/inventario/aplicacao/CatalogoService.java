package br.pucrs.pulsepass.inventario.aplicacao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Consultas de alta leitura do catálogo (US14, US15, US17). Cache Redis previsto para o Pacote 4. */
@Service
public class CatalogoService {

  private final JdbcTemplate jdbc;

  public CatalogoService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record EventoResumo(UUID id, String nome, String localCidade, Instant inicio, String estado,
      Long precoMinimoCentavos, int disponiveis) {
  }

  public record SetorDetalhe(UUID id, String nome, int capacidade, int disponiveis, Long precoCentavos, String lote) {
  }

  public record EventoDetalhe(UUID id, String nome, String descricao, String localCidade, Instant inicio,
      String estado, UUID organizadorId, List<SetorDetalhe> setores) {
  }

  public record AssentoMapa(UUID id, String rotulo, String estado) {
  }

  public List<EventoResumo> listarPublicados() {
    return jdbc.query("""
        SELECT e.id, e.nome, e.local_cidade, e.inicio, e.estado,
               (SELECT min(l.preco_centavos) FROM lote l JOIN setor s ON s.id = l.setor_id
                 WHERE s.evento_id = e.id AND now() >= l.vende_de AND now() < l.vende_ate) AS preco_min,
               (SELECT count(*) FROM assento a JOIN setor s ON s.id = a.setor_id
                 WHERE s.evento_id = e.id AND a.estado = 'DISPONIVEL') AS disponiveis
          FROM evento e WHERE e.estado = 'PUBLICADO' ORDER BY e.inicio
        """, (rs, n) -> new EventoResumo(rs.getObject("id", UUID.class), rs.getString("nome"),
        rs.getString("local_cidade"), rs.getTimestamp("inicio").toInstant(), rs.getString("estado"),
        (Long) rs.getObject("preco_min"), rs.getInt("disponiveis")));
  }

  public Optional<EventoDetalhe> detalhe(UUID eventoId) {
    List<SetorDetalhe> setores = jdbc.query("""
        SELECT s.id, s.nome, s.capacidade,
               (SELECT count(*) FROM assento a WHERE a.setor_id = s.id AND a.estado = 'DISPONIVEL') AS disponiveis,
               l.preco_centavos, l.nome AS lote
          FROM setor s
          LEFT JOIN LATERAL (SELECT preco_centavos, nome FROM lote
                              WHERE setor_id = s.id AND now() >= vende_de AND now() < vende_ate
                              ORDER BY vende_de LIMIT 1) l ON true
         WHERE s.evento_id = ? ORDER BY s.nome
        """, (rs, n) -> new SetorDetalhe(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getInt("capacidade"),
        rs.getInt("disponiveis"), (Long) rs.getObject("preco_centavos"), rs.getString("lote")), eventoId);
    return jdbc.query("""
        SELECT id, nome, descricao, local_cidade, inicio, estado, organizador_id FROM evento WHERE id = ?
        """, (rs, n) -> new EventoDetalhe(rs.getObject("id", UUID.class), rs.getString("nome"), rs.getString("descricao"),
        rs.getString("local_cidade"), rs.getTimestamp("inicio").toInstant(), rs.getString("estado"),
        rs.getObject("organizador_id", UUID.class), setores), eventoId).stream().findFirst();
  }

  public List<AssentoMapa> mapa(UUID eventoId, UUID setorId) {
    return jdbc.query("""
        SELECT a.id, a.fila_numero, a.estado FROM assento a JOIN setor s ON s.id = a.setor_id
         WHERE s.evento_id = ? AND s.id = ?
         ORDER BY substring(a.fila_numero from '^[A-Z]+'), (substring(a.fila_numero from '[0-9]+$'))::int
        """, (rs, n) -> new AssentoMapa(rs.getObject("id", UUID.class), rs.getString("fila_numero"),
        rs.getString("estado")), eventoId, setorId);
  }
}
