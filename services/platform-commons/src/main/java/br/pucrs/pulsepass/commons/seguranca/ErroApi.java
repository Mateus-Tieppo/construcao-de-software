package br.pucrs.pulsepass.commons.seguranca;

import java.time.Instant;

/** Corpo padrão de erro das APIs REST. */
public record ErroApi(String codigo, String mensagem, Instant instante) {

  public static ErroApi de(String codigo, String mensagem) {
    return new ErroApi(codigo, mensagem, Instant.now());
  }
}
