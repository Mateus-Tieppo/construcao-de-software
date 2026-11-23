package br.pucrs.pulsepass.checkout.dominio;

/** Transição não prevista na FSM (ex.: evento sobre pedido Pago). */
public class TransicaoInvalidaException extends RegraNegocioException {

  public TransicaoInvalidaException(EstadoPedido de, EstadoPedido para) {
    super("TRANSICAO_INVALIDA", "Transição inválida do pedido: " + de + " -> " + para);
  }
}
