package br.pucrs.pulsepass.inventario.adaptadores.web;

import br.pucrs.pulsepass.commons.seguranca.ErroApi;
import br.pucrs.pulsepass.inventario.dominio.RegraNegocioException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

  private static final Set<String> CONFLITOS = Set.of("ASSENTO_INDISPONIVEL", "RESERVA_EXPIRADA", "LOTE_ESGOTADO",
      "EVENTO_INDISPONIVEL", "SEM_LOTE_ATIVO", "EVENTO_NAO_RASCUNHO");

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroApi> regra(RegraNegocioException e) {
    HttpStatus status = CONFLITOS.contains(e.codigo()) ? HttpStatus.CONFLICT
        : e.codigo().endsWith("INEXISTENTE") ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status).body(ErroApi.de(e.codigo(), e.getMessage()));
  }
}
