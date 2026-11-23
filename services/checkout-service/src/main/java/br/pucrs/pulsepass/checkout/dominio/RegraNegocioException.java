package br.pucrs.pulsepass.checkout.dominio;

/** Violação de regra de negócio do Checkout. */
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
