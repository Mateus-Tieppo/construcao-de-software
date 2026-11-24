package br.pucrs.pulsepass.checkout.adaptadores.clientes;

/** Falha técnica (timeout, conexão) ao chamar outro serviço. */
public class ServicoIndisponivelException extends RuntimeException {

  public ServicoIndisponivelException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
