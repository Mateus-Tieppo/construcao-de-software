package br.pucrs.pulsepass.ingressos.dominio;

/** PENDENTE → EMITIDO → UTILIZADO. O PDF é gerado de forma assíncrona. */
public enum EstadoIngresso {
  PENDENTE, EMITIDO, UTILIZADO;

  public boolean podeValidar() {
    return this == EMITIDO;
  }

  public boolean podeBaixarPdf() {
    return this != PENDENTE;
  }
}
