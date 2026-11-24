package br.pucrs.pulsepass.inventario.dominio;

/** Ciclo de vida do evento: só eventos PUBLICADOS aparecem no catálogo e aceitam reservas (US12). */
public enum EstadoEvento {
  RASCUNHO, PUBLICADO, ENCERRADO;

  public EstadoEvento publicar() {
    if (this != RASCUNHO) {
      throw new RegraNegocioException("EVENTO_NAO_RASCUNHO", "Só eventos em rascunho podem ser publicados");
    }
    return PUBLICADO;
  }

  public boolean aceitaReservas() {
    return this == PUBLICADO;
  }
}
