package br.pucrs.pulsepass.inventario.dominio;

/** Estados da reserva no Inventário (Pacote 2, documento 2, seção 2.5). */
public enum EstadoReserva {
  ATIVA, CONFIRMADA, EXPIRADA, LIBERADA;

  public boolean finalizado() {
    return this != ATIVA;
  }
}
