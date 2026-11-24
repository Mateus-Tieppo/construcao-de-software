package br.pucrs.pulsepass.checkout.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Pedido com a FSM. Domínio puro: sem Spring, banco ou Kafka (RNF34).
 * Cada método valida a transição e devolve a {@link Transicao} a ser persistida de forma condicional.
 */
public final class Pedido {

  private final UUID id;
  private final UUID compradorId;
  private final UUID eventoId;
  private UUID reservaId;
  private EstadoPedido estado;
  private MotivoExpiracao motivo;
  private long totalCentavos;
  private Instant expiraEm;
  private final long versao;

  public Pedido(UUID id, UUID compradorId, UUID eventoId, UUID reservaId, EstadoPedido estado,
      MotivoExpiracao motivo, long totalCentavos, Instant expiraEm, long versao) {
    this.id = Objects.requireNonNull(id);
    this.compradorId = Objects.requireNonNull(compradorId);
    this.eventoId = Objects.requireNonNull(eventoId);
    this.reservaId = reservaId;
    this.estado = Objects.requireNonNull(estado);
    this.motivo = motivo;
    this.totalCentavos = totalCentavos;
    this.expiraEm = expiraEm;
    this.versao = versao;
  }

  public static Pedido novo(UUID id, UUID compradorId, UUID eventoId) {
    return new Pedido(id, compradorId, eventoId, null, EstadoPedido.CRIADO, null, 0, null, 1);
  }

  /** Criado → Reservado: o Inventário reservou todos os assentos. */
  public Transicao reservar(UUID reserva, Instant expira, long total) {
    if (reserva == null || expira == null || total <= 0) {
      throw new RegraNegocioException("RESERVA_INVALIDA", "Reserva sem id, prazo ou valor");
    }
    Transicao t = mudar(EstadoPedido.RESERVADO, null);
    this.reservaId = reserva;
    this.expiraEm = expira;
    this.totalCentavos = total;
    return t;
  }

  /** Criado → Expirado (FALHA_RESERVA). */
  public Transicao falharReserva() {
    exigirEstado(EstadoPedido.CRIADO);
    return mudar(EstadoPedido.EXPIRADO, MotivoExpiracao.FALHA_RESERVA);
  }

  /** Reservado → Aguardando, só dentro do prazo da reserva (checagem local; o Inventário decide depois). */
  public Transicao iniciarPagamento(Instant agora) {
    exigirEstado(EstadoPedido.RESERVADO);
    if (!agora.isBefore(expiraEm)) {
      throw new RegraNegocioException("PRAZO_ENCERRADO", "O prazo de 10 minutos da reserva terminou");
    }
    return mudar(EstadoPedido.AGUARDANDO, null);
  }

  /** Aguardando → Pago: autorizado, reserva confirmada no Inventário e capturado. */
  public Transicao confirmarPagamento() {
    exigirEstado(EstadoPedido.AGUARDANDO);
    return mudar(EstadoPedido.PAGO, null);
  }

  /** Aguardando → Reservado se o gateway recusou e ainda há prazo; senão Expirado por TIMEOUT (US24). */
  public Transicao recusarPagamento(Instant agora) {
    exigirEstado(EstadoPedido.AGUARDANDO);
    if (agora.isBefore(expiraEm)) {
      return mudar(EstadoPedido.RESERVADO, null);
    }
    return mudar(EstadoPedido.EXPIRADO, MotivoExpiracao.TIMEOUT);
  }

  /** Aguardando → Expirado: o Inventário recusou a confirmação porque a reserva venceu. */
  public Transicao pagamentoTardio() {
    exigirEstado(EstadoPedido.AGUARDANDO);
    return mudar(EstadoPedido.EXPIRADO, MotivoExpiracao.PAGAMENTO_TARDIO);
  }

  /** Reservado ou Aguardando → Expirado (TIMEOUT), ao consumir ReservaExpirada. */
  public Transicao expirarPorTempo() {
    if (estado != EstadoPedido.RESERVADO && estado != EstadoPedido.AGUARDANDO) {
      throw new TransicaoInvalidaException(estado, EstadoPedido.EXPIRADO);
    }
    return mudar(EstadoPedido.EXPIRADO, MotivoExpiracao.TIMEOUT);
  }

  /** Reservado → Expirado (CANCELADO_USUARIO). */
  public Transicao cancelar() {
    exigirEstado(EstadoPedido.RESERVADO);
    return mudar(EstadoPedido.EXPIRADO, MotivoExpiracao.CANCELADO_USUARIO);
  }

  public boolean pertenceA(UUID usuario) {
    return compradorId.equals(usuario);
  }

  private void exigirEstado(EstadoPedido esperado) {
    if (estado != esperado) {
      throw new TransicaoInvalidaException(estado, esperado == EstadoPedido.CRIADO
          ? EstadoPedido.EXPIRADO : proximoNatural(esperado));
    }
  }

  private static EstadoPedido proximoNatural(EstadoPedido esperado) {
    return switch (esperado) {
      case RESERVADO -> EstadoPedido.AGUARDANDO;
      case AGUARDANDO -> EstadoPedido.PAGO;
      default -> EstadoPedido.EXPIRADO;
    };
  }

  private Transicao mudar(EstadoPedido destino, MotivoExpiracao motivoNovo) {
    if (!estado.podeIrPara(destino)) {
      throw new TransicaoInvalidaException(estado, destino);
    }
    Transicao t = new Transicao(estado, destino, motivoNovo);
    this.estado = destino;
    this.motivo = motivoNovo;
    return t;
  }

  public UUID id() {
    return id;
  }

  public UUID compradorId() {
    return compradorId;
  }

  public UUID eventoId() {
    return eventoId;
  }

  public UUID reservaId() {
    return reservaId;
  }

  public EstadoPedido estado() {
    return estado;
  }

  public MotivoExpiracao motivo() {
    return motivo;
  }

  public long totalCentavos() {
    return totalCentavos;
  }

  public Instant expiraEm() {
    return expiraEm;
  }

  public long versao() {
    return versao;
  }
}
