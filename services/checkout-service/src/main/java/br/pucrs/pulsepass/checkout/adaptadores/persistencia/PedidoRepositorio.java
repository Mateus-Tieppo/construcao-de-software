package br.pucrs.pulsepass.checkout.adaptadores.persistencia;

import br.pucrs.pulsepass.checkout.dominio.EstadoPedido;
import br.pucrs.pulsepass.checkout.dominio.MotivoExpiracao;
import br.pucrs.pulsepass.checkout.dominio.Pedido;
import br.pucrs.pulsepass.checkout.dominio.Transicao;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistência do pedido. Toda transição é um UPDATE condicional por estado (seção 2.4 do Pacote 2). */
@Repository
public class PedidoRepositorio {

  private final JdbcTemplate jdbc;

  public PedidoRepositorio(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Item(UUID id, UUID assentoId, UUID loteId, long precoCentavos, String descricao) {
  }

  public record Pagamento(UUID id, String estado, long valorCentavos, String referenciaGateway, Instant criadoEm) {
  }

  public void inserir(Pedido p, String compradorNome) {
    jdbc.update("""
        INSERT INTO pedido (id, comprador_id, comprador_nome, evento_id, estado, total_centavos, versao, criado_em)
        VALUES (?, ?, ?, ?, ?, 0, 1, now())
        """, p.id(), p.compradorId(), compradorNome, p.eventoId(), p.estado().name());
  }

  /** Aplica a transição só se o pedido ainda estiver no estado de origem. */
  public boolean aplicar(Pedido p, Transicao t) {
    int linhas = jdbc.update("""
        UPDATE pedido SET estado = ?, motivo_expiracao = ?, reserva_id = ?, total_centavos = ?, expira_em = ?,
               versao = versao + 1, atualizado_em = now()
         WHERE id = ? AND estado = ?
        """, t.para().name(), t.motivo() == null ? null : t.motivo().name(), p.reservaId(), p.totalCentavos(),
        p.expiraEm() == null ? null : Timestamp.from(p.expiraEm()), p.id(), t.de().name());
    return linhas == 1;
  }

  public Optional<Pedido> buscar(UUID id) {
    return jdbc.query("SELECT * FROM pedido WHERE id = ?", this::mapear, id).stream().findFirst();
  }

  public Optional<Pedido> buscarPorReserva(UUID reservaId) {
    return jdbc.query("SELECT * FROM pedido WHERE reserva_id = ?", this::mapear, reservaId).stream().findFirst();
  }

  public List<Pedido> doComprador(UUID compradorId) {
    return jdbc.query("SELECT * FROM pedido WHERE comprador_id = ? ORDER BY criado_em DESC LIMIT 50", this::mapear,
        compradorId);
  }

  public String compradorNome(UUID pedidoId) {
    return jdbc.queryForObject("SELECT comprador_nome FROM pedido WHERE id = ?", String.class, pedidoId);
  }

  public void inserirItens(UUID pedidoId, List<Item> itens) {
    for (Item i : itens) {
      jdbc.update("""
          INSERT INTO item_pedido (id, pedido_id, assento_id, lote_id, preco_centavos, descricao)
          VALUES (?, ?, ?, ?, ?, ?)
          """, i.id(), pedidoId, i.assentoId(), i.loteId(), i.precoCentavos(), i.descricao());
    }
  }

  public List<Item> itens(UUID pedidoId) {
    return jdbc.query("SELECT * FROM item_pedido WHERE pedido_id = ? ORDER BY descricao", (rs, n) -> new Item(
        rs.getObject("id", UUID.class), rs.getObject("assento_id", UUID.class), rs.getObject("lote_id", UUID.class),
        rs.getLong("preco_centavos"), rs.getString("descricao")), pedidoId);
  }

  public void inserirPagamento(UUID pagamentoId, UUID pedidoId, long valor) {
    jdbc.update("""
        INSERT INTO pagamento (id, pedido_id, estado, valor_centavos, criado_em) VALUES (?, ?, 'AUTORIZANDO', ?, now())
        """, pagamentoId, pedidoId, valor);
  }

  public void atualizarPagamento(UUID pagamentoId, String estado, String referencia) {
    jdbc.update("UPDATE pagamento SET estado = ?, referencia_gateway = coalesce(?, referencia_gateway) WHERE id = ?",
        estado, referencia, pagamentoId);
  }

  public Optional<Pagamento> ultimoPagamento(UUID pedidoId) {
    return jdbc.query("SELECT * FROM pagamento WHERE pedido_id = ? ORDER BY criado_em DESC LIMIT 1",
        (rs, n) -> new Pagamento(rs.getObject("id", UUID.class), rs.getString("estado"), rs.getLong("valor_centavos"),
            rs.getString("referencia_gateway"), rs.getTimestamp("criado_em").toInstant()), pedidoId).stream().findFirst();
  }

  private Pedido mapear(ResultSet rs, int n) throws SQLException {
    String motivo = rs.getString("motivo_expiracao");
    Timestamp expira = rs.getTimestamp("expira_em");
    return new Pedido(rs.getObject("id", UUID.class), rs.getObject("comprador_id", UUID.class),
        rs.getObject("evento_id", UUID.class), rs.getObject("reserva_id", UUID.class),
        EstadoPedido.valueOf(rs.getString("estado")), motivo == null ? null : MotivoExpiracao.valueOf(motivo),
        rs.getLong("total_centavos"), expira == null ? null : expira.toInstant(), rs.getLong("versao"));
  }
}
