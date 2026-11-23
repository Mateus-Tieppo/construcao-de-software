package br.pucrs.pulsepass.checkout.dominio;

/** Resultado de uma transição da FSM; o repositório aplica com UPDATE ... WHERE estado = de. */
public record Transicao(EstadoPedido de, EstadoPedido para, MotivoExpiracao motivo) {
}
