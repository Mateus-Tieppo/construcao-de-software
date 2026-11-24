package br.pucrs.pulsepass.checkout.aplicacao;

import br.pucrs.pulsepass.checkout.adaptadores.clientes.GatewayPagamentoClient;
import br.pucrs.pulsepass.checkout.adaptadores.clientes.InventarioClient;
import br.pucrs.pulsepass.checkout.adaptadores.clientes.ServicoIndisponivelException;
import br.pucrs.pulsepass.checkout.adaptadores.persistencia.PedidoRepositorio;
import br.pucrs.pulsepass.checkout.dominio.EstadoPedido;
import br.pucrs.pulsepass.checkout.dominio.Pedido;
import br.pucrs.pulsepass.checkout.dominio.RegraNegocioException;
import br.pucrs.pulsepass.checkout.dominio.Transicao;
import br.pucrs.pulsepass.commons.eventos.Uuids;
import br.pucrs.pulsepass.commons.outbox.OutboxWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Casos de uso CriarPedidoComReserva, PagarPedido, CancelarPedido e AplicarExpiracao (C3, Figura 5).
 * Chamadas remotas ficam FORA das transações; cada mudança de estado grava o evento na Outbox
 * na mesma transação (RNF06).
 */
@Service
public class PedidoService {

  public static final String TOPICO_PEDIDOS = "checkout.pedidos.v1";
  private static final Logger LOG = LoggerFactory.getLogger(PedidoService.class);

  private final PedidoRepositorio repo;
  private final InventarioClient inventario;
  private final GatewayPagamentoClient gateway;
  private final OutboxWriter outbox;
  private final TransactionTemplate tx;

  public PedidoService(PedidoRepositorio repo, InventarioClient inventario, GatewayPagamentoClient gateway,
      OutboxWriter outbox, TransactionTemplate tx) {
    this.repo = repo;
    this.inventario = inventario;
    this.gateway = gateway;
    this.outbox = outbox;
    this.tx = tx;
  }

  public PedidoView criar(UUID compradorId, String compradorNome, UUID eventoId, List<UUID> assentos) {
    if (eventoId == null) {
      throw new RegraNegocioException("EVENTO_OBRIGATORIO", "Informe o evento");
    }
    Pedido pedido = Pedido.novo(Uuids.v7(), compradorId, eventoId);
    tx.executeWithoutResult(s -> repo.inserir(pedido, compradorNome));

    InventarioClient.Reserva reserva;
    try {
      reserva = inventario.reservar(pedido.id(), compradorId, eventoId, assentos);
    } catch (RegraNegocioException | ServicoIndisponivelException e) {
      Transicao t = pedido.falharReserva();
      tx.executeWithoutResult(s -> {
        repo.aplicar(pedido, t);
        publicarExpirado(pedido);
      });
      throw e;
    }

    Transicao t = pedido.reservar(reserva.reservaId(), reserva.expiraEm(), reserva.totalCentavos());
    tx.executeWithoutResult(s -> {
      repo.aplicar(pedido, t);
      repo.inserirItens(pedido.id(), reserva.itens().stream().map(i -> new PedidoRepositorio.Item(Uuids.v7(),
          i.assentoId(), i.loteId(), i.precoCentavos(), i.descricao())).toList());
      Map<String, Object> dados = base(pedido);
      dados.put("expiraEm", pedido.expiraEm());
      outbox.gravar(TOPICO_PEDIDOS, "PedidoReservado", pedido.id(), 2, pedido.id(), pedido.id(), dados);
    });
    return visao(pedido.id(), null);
  }

  public PedidoView pagar(UUID pedidoId, UUID compradorId, String cartaoFinal) {
    Pedido pedido = doComprador(pedidoId, compradorId);
    if (pedido.estado() == EstadoPedido.PAGO) {
      return visao(pedidoId, "Pedido já pago");
    }
    Transicao iniciar = pedido.iniciarPagamento(Instant.now());
    UUID pagamentoId = Uuids.v7();
    boolean ok = Boolean.TRUE.equals(tx.execute(s -> {
      if (!repo.aplicar(pedido, iniciar)) {
        return false;
      }
      repo.inserirPagamento(pagamentoId, pedido.id(), pedido.totalCentavos());
      Map<String, Object> dados = base(pedido);
      dados.put("pagamentoId", pagamentoId);
      outbox.gravar(TOPICO_PEDIDOS, "PagamentoIniciado", pedido.id(), 3, pedido.id(), pagamentoId, dados);
      return true;
    }));
    if (!ok) {
      throw new RegraNegocioException("ESTADO_ALTERADO", "O pedido mudou de estado; consulte novamente");
    }

    GatewayPagamentoClient.Autorizacao autorizacao;
    try {
      autorizacao = gateway.autorizar(pagamentoId, pedido.totalCentavos(), cartaoFinal);
    } catch (ServicoIndisponivelException e) {
      autorizacao = new GatewayPagamentoClient.Autorizacao(pagamentoId, "RECUSADO", null, "Gateway indisponível");
    }
    if (!autorizacao.autorizado()) {
      return recusar(pedido, pagamentoId, autorizacao);
    }
    repo.atualizarPagamento(pagamentoId, "AUTORIZADO", autorizacao.referencia());

    try {
      inventario.confirmar(pedido.reservaId());
    } catch (RegraNegocioException e) {
      return pagamentoTardio(pedido, pagamentoId);
    }
    gateway.capturar(pagamentoId);

    Transicao pagar = pedido.confirmarPagamento();
    tx.executeWithoutResult(s -> {
      repo.aplicar(pedido, pagar);
      repo.atualizarPagamento(pagamentoId, "CAPTURADO", null);
      Map<String, Object> dados = base(pedido);
      dados.put("pagamentoId", pagamentoId);
      dados.put("compradorNome", repo.compradorNome(pedido.id()));
      dados.put("itens", repo.itens(pedido.id()).stream().map(i -> Map.of("itemPedidoId", i.id(),
          "assentoId", i.assentoId(), "descricao", i.descricao(), "precoCentavos", i.precoCentavos())).toList());
      dados.put("pagoEm", Instant.now());
      outbox.gravar(TOPICO_PEDIDOS, "PagamentoConfirmado", pedido.id(), 4, pedido.id(), pagamentoId, dados);
    });
    return visao(pedidoId, "Pagamento confirmado");
  }

  private PedidoView recusar(Pedido pedido, UUID pagamentoId, GatewayPagamentoClient.Autorizacao autorizacao) {
    Transicao t = pedido.recusarPagamento(Instant.now());
    tx.executeWithoutResult(s -> {
      repo.aplicar(pedido, t);
      repo.atualizarPagamento(pagamentoId, "RECUSADO", autorizacao.referencia());
      Map<String, Object> dados = base(pedido);
      dados.put("pagamentoId", pagamentoId);
      outbox.gravar(TOPICO_PEDIDOS, "PagamentoRecusado", pedido.id(), 3, pedido.id(), pagamentoId, dados);
      if (t.para() == EstadoPedido.EXPIRADO) {
        publicarExpirado(pedido);
      }
    });
    return visao(pedido.id(), autorizacao.mensagem() == null ? "Pagamento recusado" : autorizacao.mensagem());
  }

  private PedidoView pagamentoTardio(Pedido pedido, UUID pagamentoId) {
    try {
      gateway.cancelar(pagamentoId);
    } catch (ServicoIndisponivelException e) {
      LOG.warn("Cancelamento da autorização {} pendente: {}", pagamentoId, e.getMessage());
    }
    Transicao t = pedido.pagamentoTardio();
    tx.executeWithoutResult(s -> {
      repo.atualizarPagamento(pagamentoId, "CANCELADO", null);
      if (repo.aplicar(pedido, t)) {
        publicarExpirado(pedido);
      }
    });
    return visao(pedido.id(), "A reserva expirou antes da confirmação; a autorização foi cancelada");
  }

  public PedidoView cancelar(UUID pedidoId, UUID compradorId) {
    Pedido pedido = doComprador(pedidoId, compradorId);
    Transicao t = pedido.cancelar();
    tx.executeWithoutResult(s -> {
      if (repo.aplicar(pedido, t)) {
        publicarExpirado(pedido);
      }
    });
    try {
      inventario.liberar(pedido.reservaId());
    } catch (RuntimeException e) {
      // o consumidor inv-pedidos libera a reserva ao ler PedidoExpirado
      LOG.warn("Liberação síncrona falhou para o pedido {}: {}", pedidoId, e.getMessage());
    }
    return visao(pedidoId, "Pedido cancelado");
  }

  /** Consumo de ReservaExpirada: leva o pedido não pago a Expirado (TIMEOUT). Chamado dentro da transação da Inbox. */
  public boolean aplicarExpiracao(UUID pedidoId) {
    Pedido pedido = repo.buscar(pedidoId).orElse(null);
    if (pedido == null || pedido.estado().finalizado()) {
      LOG.info("ReservaExpirada ignorada para pedido {} (estado final ou inexistente)", pedidoId);
      return false;
    }
    Transicao t = pedido.expirarPorTempo();
    if (repo.aplicar(pedido, t)) {
      publicarExpirado(pedido);
      return true;
    }
    return false;
  }

  public PedidoView consultar(UUID pedidoId, UUID compradorId) {
    doComprador(pedidoId, compradorId);
    return visao(pedidoId, null);
  }

  public List<PedidoView> meus(UUID compradorId) {
    return repo.doComprador(compradorId).stream().map(p -> visao(p.id(), null)).toList();
  }

  private Pedido doComprador(UUID pedidoId, UUID compradorId) {
    Pedido p = repo.buscar(pedidoId).orElse(null);
    if (p == null || !p.pertenceA(compradorId)) {
      // invariante I4: só o comprador acessa o próprio pedido (404 para não revelar existência)
      throw new RegraNegocioException("PEDIDO_INEXISTENTE", "Pedido não encontrado");
    }
    return p;
  }

  private void publicarExpirado(Pedido pedido) {
    Map<String, Object> dados = base(pedido);
    dados.put("motivo", pedido.motivo() == null ? null : pedido.motivo().name());
    outbox.gravar(TOPICO_PEDIDOS, "PedidoExpirado", pedido.id(), 5, pedido.id(), pedido.id(), dados);
  }

  private Map<String, Object> base(Pedido p) {
    Map<String, Object> dados = new LinkedHashMap<>();
    dados.put("pedidoId", p.id());
    dados.put("reservaId", p.reservaId());
    dados.put("compradorId", p.compradorId());
    dados.put("eventoId", p.eventoId());
    dados.put("estado", p.estado().name());
    dados.put("totalCentavos", p.totalCentavos());
    return dados;
  }

  private PedidoView visao(UUID pedidoId, String mensagem) {
    Pedido p = repo.buscar(pedidoId).orElseThrow();
    return new PedidoView(p.id(), p.eventoId(), p.reservaId(), p.estado().name(),
        p.motivo() == null ? null : p.motivo().name(), p.totalCentavos(), p.expiraEm(), repo.itens(p.id()),
        repo.ultimoPagamento(p.id()).map(PedidoRepositorio.Pagamento::estado).orElse(null), mensagem);
  }
}
