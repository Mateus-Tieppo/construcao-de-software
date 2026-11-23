package br.pucrs.pulsepass.checkout.dominio;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados da máquina de estados finitos do pedido exigida pelo enunciado:
 * Criado, Reservado, Aguardando, Pago, Expirado (Pacote 2, documento 2, Figura 18).
 */
public enum EstadoPedido {
  CRIADO, RESERVADO, AGUARDANDO, PAGO, EXPIRADO;

  /** Transições permitidas. Pago e Expirado são finais (invariante I7). */
  public Set<EstadoPedido> destinosPermitidos() {
    return switch (this) {
      case CRIADO -> EnumSet.of(RESERVADO, EXPIRADO);
      case RESERVADO -> EnumSet.of(AGUARDANDO, EXPIRADO);
      case AGUARDANDO -> EnumSet.of(PAGO, RESERVADO, EXPIRADO);
      default -> EnumSet.noneOf(EstadoPedido.class);
    };
  }

  public boolean podeIrPara(EstadoPedido destino) {
    return destinosPermitidos().contains(destino);
  }

  public boolean finalizado() {
    return this == PAGO || this == EXPIRADO;
  }
}
