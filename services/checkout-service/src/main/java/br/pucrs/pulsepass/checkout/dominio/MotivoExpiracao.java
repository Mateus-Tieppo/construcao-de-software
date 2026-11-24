package br.pucrs.pulsepass.checkout.dominio;

/** Motivos de Expirado: cancelamento, falha e pagamento tardio são motivos, não estados novos. */
public enum MotivoExpiracao {
  TIMEOUT, CANCELADO_USUARIO, FALHA_RESERVA, PAGAMENTO_TARDIO
}
