package br.pucrs.pulsepass.simulador;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimuladorController {

  private final Simulador simulador;

  public SimuladorController(Simulador simulador) {
    this.simulador = simulador;
  }

  public record PedidoAutorizacao(UUID pagamentoId, long valorCentavos, String cartaoFinal) {
  }

  @PostMapping("/pagamentos/autorizacoes")
  public Simulador.Transacao autorizar(@RequestBody PedidoAutorizacao req) {
    return simulador.autorizar(req.pagamentoId(), req.valorCentavos(), req.cartaoFinal());
  }

  @PostMapping("/pagamentos/{id}/captura")
  public Simulador.Transacao capturar(@PathVariable UUID id) {
    return simulador.capturar(id);
  }

  @PostMapping("/pagamentos/{id}/cancelamento")
  public Simulador.Transacao cancelar(@PathVariable UUID id) {
    return simulador.cancelar(id);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> naoEncontrado(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }
}
