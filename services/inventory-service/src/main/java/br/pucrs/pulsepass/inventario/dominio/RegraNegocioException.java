package br.pucrs.pulsepass.inventario.dominio;

/** Violação de regra de negócio; o código vai na resposta HTTP. */
public class RegraNegocioException extends RuntimeException {

  private final String codigo;

  public RegraNegocioException(String codigo, String mensagem) {
    super(mensagem);
    this.codigo = codigo;
  }

  public String codigo() {
    return codigo;
  }
}
