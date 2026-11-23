package br.pucrs.pulsepass.checkout.adaptadores.web;

import br.pucrs.pulsepass.checkout.adaptadores.clientes.ServicoIndisponivelException;
import br.pucrs.pulsepass.checkout.dominio.RegraNegocioException;
import br.pucrs.pulsepass.checkout.dominio.TransicaoInvalidaException;
import br.pucrs.pulsepass.commons.seguranca.ErroApi;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

  private static final Set<String> CONFLITOS = Set.of("ASSENTO_INDISPONIVEL", "RESERVA_EXPIRADA", "LOTE_ESGOTADO",
      "EVENTO_INDISPONIVEL", "SEM_LOTE_ATIVO", "PRAZO_ENCERRADO", "ESTADO_ALTERADO");

  @ExceptionHandler(TransicaoInvalidaException.class)
  public ResponseEntity<ErroApi> transicao(TransicaoInvalidaException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroApi.de(e.codigo(), e.getMessage()));
  }

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroApi> regra(RegraNegocioException e) {
    HttpStatus status = CONFLITOS.contains(e.codigo()) ? HttpStatus.CONFLICT
        : e.codigo().endsWith("INEXISTENTE") ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status).body(ErroApi.de(e.codigo(), e.getMessage()));
  }

  @ExceptionHandler(ServicoIndisponivelException.class)
  public ResponseEntity<ErroApi> indisponivel(ServicoIndisponivelException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErroApi.de("SERVICO_INDISPONIVEL", e.getMessage()));
  }
}
