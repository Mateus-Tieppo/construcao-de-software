package br.pucrs.pulsepass.inventario.dominio;

/** Estados do assento: DISPONIVEL ⇄ RESERVADO → VENDIDO; BLOQUEADO fica fora de venda. */
public enum EstadoAssento {
  DISPONIVEL, RESERVADO, VENDIDO, BLOQUEADO;

  public boolean podeIrPara(EstadoAssento destino) {
    return switch (this) {
      case DISPONIVEL -> destino == RESERVADO || destino == BLOQUEADO;
      case RESERVADO -> destino == DISPONIVEL || destino == VENDIDO;
      case BLOQUEADO -> destino == DISPONIVEL;
      default -> false;
    };
  }
}
