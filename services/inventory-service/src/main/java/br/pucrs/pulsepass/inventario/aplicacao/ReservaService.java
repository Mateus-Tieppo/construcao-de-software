package br.pucrs.pulsepass.inventario.aplicacao;

import br.pucrs.pulsepass.commons.eventos.Uuids;
import br.pucrs.pulsepass.commons.outbox.OutboxWriter;
import br.pucrs.pulsepass.inventario.dominio.EstadoEvento;
import br.pucrs.pulsepass.inventario.dominio.EstadoReserva;
import br.pucrs.pulsepass.inventario.dominio.PoliticaReserva;
import br.pucrs.pulsepass.inventario.dominio.RegraNegocioException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso ReservarAssentos, ConfirmarReserva e LiberarReserva (C3, Figura 4).
 * A decisão de posse do assento é sempre um UPDATE condicional no PostgreSQL (ADR-002): zero overselling.
 */
@Service
public class ReservaService {

  public static final String TOPICO_RESERVAS = "inventario.reservas.v1";

  private final JdbcTemplate jdbc;
  private final OutboxWriter outbox;

  public ReservaService(JdbcTemplate jdbc, OutboxWriter outbox) {
    this.jdbc = jdbc;
    this.outbox = outbox;
  }

  @Transactional
  public ReservaResultado reservar(UUID pedidoId, UUID compradorId, UUID eventoId, List<UUID> assentosPedidos) {
    List<UUID> assentos = PoliticaReserva.validarEOrdenar(assentosPedidos);

    Optional<ReservaResultado> existente = buscarPorPedido(pedidoId);
    if (existente.isPresent()) {
      return existente.get(); // idempotente por pedidoId
    }
    String estadoEvento = jdbc.query("SELECT estado FROM evento WHERE id = ?",
        rs -> rs.next() ? rs.getString(1) : null, eventoId);
    if (estadoEvento == null || !EstadoEvento.valueOf(estadoEvento).aceitaReservas()) {
      throw new RegraNegocioException("EVENTO_INDISPONIVEL", "Evento inexistente ou fora de venda");
    }

    UUID reservaId = Uuids.v7();
    Instant agora = Instant.now();
    Instant expiraEm = PoliticaReserva.expiracao(agora);
    jdbc.update("""
        INSERT INTO reserva (id, pedido_id, comprador_id, evento_id, estado, criada_em, expira_em, versao)
        VALUES (?, ?, ?, ?, 'ATIVA', ?, ?, 1)
        """, reservaId, pedidoId, compradorId, eventoId, Timestamp.from(agora), Timestamp.from(expiraEm));

    int bloqueados = 0;
    for (UUID assentoId : assentos) {
      // bloqueio de linha do PostgreSQL, sempre na mesma ordem (ids ordenados)
      bloqueados += jdbc.update("""
          UPDATE assento a SET estado = 'RESERVADO', reserva_id = ?, versao = versao + 1
            FROM setor s
           WHERE a.id = ? AND a.setor_id = s.id AND s.evento_id = ? AND a.estado = 'DISPONIVEL'
          """, reservaId, assentoId, eventoId);
    }
    PoliticaReserva.exigirTodosBloqueados(assentos.size(), bloqueados);

    List<ReservaResultado.Item> itens = new ArrayList<>();
    for (UUID assentoId : assentos) {
      Map<String, Object> lote = jdbc.query("""
          SELECT l.id, l.preco_centavos, s.nome AS setor, a.fila_numero
            FROM assento a JOIN setor s ON s.id = a.setor_id JOIN lote l ON l.setor_id = s.id
           WHERE a.id = ? AND now() >= l.vende_de AND now() < l.vende_ate
           ORDER BY l.vende_de LIMIT 1
          """, rs -> {
            if (!rs.next()) {
              return null;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getObject("id", UUID.class));
            m.put("preco", rs.getLong("preco_centavos"));
            m.put("descricao", rs.getString("setor") + " " + rs.getString("fila_numero"));
            return m;
          }, assentoId);
      if (lote == null) {
        throw new RegraNegocioException("SEM_LOTE_ATIVO", "Não há lote em venda para o setor do assento");
      }
      UUID loteId = (UUID) lote.get("id");
      int ok = jdbc.update("""
          UPDATE lote SET reservados = reservados + 1
           WHERE id = ? AND reservados + vendidos + 1 <= quantidade
          """, loteId);
      if (ok == 0) {
        throw new RegraNegocioException("LOTE_ESGOTADO", "Lote esgotado");
      }
      long preco = (Long) lote.get("preco");
      String descricao = (String) lote.get("descricao");
      jdbc.update("INSERT INTO item_reserva (reserva_id, assento_id, lote_id, preco_centavos, descricao) VALUES (?, ?, ?, ?, ?)",
          reservaId, assentoId, loteId, preco, descricao);
      itens.add(new ReservaResultado.Item(assentoId, loteId, preco, descricao));
    }
    long total = itens.stream().mapToLong(ReservaResultado.Item::precoCentavos).sum();
    ReservaResultado resultado = new ReservaResultado(reservaId, pedidoId, eventoId, EstadoReserva.ATIVA.name(),
        expiraEm, total, itens);
    outbox.gravar(TOPICO_RESERVAS, "ReservaCriada", reservaId, 1, pedidoId, pedidoId, resultado);
    return resultado;
  }

  /**
   * Confirma a reserva só se ainda estiver ATIVA e dentro do prazo, pelo relógio do banco (ADR-003, invariante I3).
   */
  @Transactional
  public ReservaResultado confirmar(UUID reservaId) {
    int linhas = jdbc.update("""
        UPDATE reserva SET estado = 'CONFIRMADA', versao = versao + 1
         WHERE id = ? AND estado = 'ATIVA' AND now() < expira_em
        """, reservaId);
    ReservaResultado reserva = buscar(reservaId)
        .orElseThrow(() -> new RegraNegocioException("RESERVA_INEXISTENTE", "Reserva não encontrada"));
    if (linhas == 0) {
      if (EstadoReserva.CONFIRMADA.name().equals(reserva.estado())) {
        return reserva; // repetição da mesma confirmação
      }
      throw new RegraNegocioException("RESERVA_EXPIRADA", "A reserva expirou ou foi liberada");
    }
    jdbc.update("UPDATE assento SET estado = 'VENDIDO', versao = versao + 1 WHERE reserva_id = ? AND estado = 'RESERVADO'",
        reservaId);
    jdbc.update("""
        UPDATE lote l SET reservados = reservados - x.n, vendidos = vendidos + x.n
          FROM (SELECT lote_id, count(*) AS n FROM item_reserva WHERE reserva_id = ? GROUP BY lote_id) x
         WHERE l.id = x.lote_id
        """, reservaId);
    outbox.gravar(TOPICO_RESERVAS, "ReservaConfirmada", reservaId, versao(reservaId), reserva.pedidoId(), reservaId,
        Map.of("reservaId", reservaId, "pedidoId", reserva.pedidoId(), "eventoId", reserva.eventoId()));
    return buscar(reservaId).orElseThrow();
  }

  /**
   * Devolve atomicamente todos os assentos da reserva (cancelamento, falha ou expiração).
   * @param somenteVencida true quando chamado pelo worker de expiração
   * @return true se esta chamada liberou a reserva
   */
  @Transactional
  public boolean liberar(UUID reservaId, boolean somenteVencida) {
    EstadoReserva destino = somenteVencida ? EstadoReserva.EXPIRADA : EstadoReserva.LIBERADA;
    int linhas = jdbc.update("UPDATE reserva SET estado = ?, versao = versao + 1 WHERE id = ? AND estado = 'ATIVA'"
        + (somenteVencida ? " AND expira_em <= now()" : ""), destino.name(), reservaId);
    if (linhas == 0) {
      return false;
    }
    jdbc.update("""
        UPDATE lote l SET reservados = reservados - x.n
          FROM (SELECT lote_id, count(*) AS n FROM item_reserva WHERE reserva_id = ? GROUP BY lote_id) x
         WHERE l.id = x.lote_id
        """, reservaId);
    jdbc.update("""
        UPDATE assento SET estado = 'DISPONIVEL', reserva_id = NULL, versao = versao + 1
         WHERE reserva_id = ? AND estado = 'RESERVADO'
        """, reservaId);
    ReservaResultado reserva = buscar(reservaId).orElseThrow();
    String tipo = somenteVencida ? "ReservaExpirada" : "ReservaLiberada";
    Map<String, Object> dados = new LinkedHashMap<>();
    dados.put("reservaId", reservaId);
    dados.put("pedidoId", reserva.pedidoId());
    dados.put("eventoId", reserva.eventoId());
    dados.put("assentos", reserva.itens().stream().map(ReservaResultado.Item::assentoId).toList());
    dados.put("motivo", somenteVencida ? "TIMEOUT" : "LIBERADA");
    outbox.gravar(TOPICO_RESERVAS, tipo, reservaId, versao(reservaId), reserva.pedidoId(), reservaId, dados);
    return true;
  }

  @Transactional
  public boolean liberarPorPedido(UUID pedidoId) {
    UUID reservaId = jdbc.query("SELECT id FROM reserva WHERE pedido_id = ?",
        rs -> rs.next() ? rs.getObject(1, UUID.class) : null, pedidoId);
    return reservaId != null && liberar(reservaId, false);
  }

  public Optional<ReservaResultado> buscarPorPedido(UUID pedidoId) {
    UUID id = jdbc.query("SELECT id FROM reserva WHERE pedido_id = ?",
        rs -> rs.next() ? rs.getObject(1, UUID.class) : null, pedidoId);
    return id == null ? Optional.empty() : buscar(id);
  }

  public Optional<ReservaResultado> buscar(UUID reservaId) {
    List<ReservaResultado.Item> itens = jdbc.query("""
        SELECT assento_id, lote_id, preco_centavos, descricao FROM item_reserva WHERE reserva_id = ? ORDER BY assento_id
        """, (rs, n) -> new ReservaResultado.Item(rs.getObject("assento_id", UUID.class),
        rs.getObject("lote_id", UUID.class), rs.getLong("preco_centavos"), rs.getString("descricao")), reservaId);
    List<ReservaResultado> r = jdbc.query("""
        SELECT id, pedido_id, evento_id, estado, expira_em FROM reserva WHERE id = ?
        """, (rs, n) -> new ReservaResultado(rs.getObject("id", UUID.class), rs.getObject("pedido_id", UUID.class),
        rs.getObject("evento_id", UUID.class), rs.getString("estado"), rs.getTimestamp("expira_em").toInstant(),
        itens.stream().mapToLong(ReservaResultado.Item::precoCentavos).sum(), itens), reservaId);
    return r.stream().findFirst();
  }

  private long versao(UUID reservaId) {
    Long v = jdbc.queryForObject("SELECT versao FROM reserva WHERE id = ?", Long.class, reservaId);
    return v == null ? 0 : v;
  }
}
