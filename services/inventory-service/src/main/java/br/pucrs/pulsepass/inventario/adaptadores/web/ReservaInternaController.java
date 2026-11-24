package br.pucrs.pulsepass.inventario.adaptadores.web;

import br.pucrs.pulsepass.inventario.aplicacao.ReservaResultado;
import br.pucrs.pulsepass.inventario.aplicacao.ReservaService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API interna usada só pelo Checkout (rede Docker interna; o Gateway não roteia /internal).
 * Autenticação por client credentials está prevista para o Pacote 4.
 */
@RestController
@RequestMapping("/internal/reservas")
public class ReservaInternaController {

  private final ReservaService reservas;

  public ReservaInternaController(ReservaService reservas) {
    this.reservas = reservas;
  }

  public record PedidoReserva(UUID pedidoId, UUID compradorId, UUID eventoId, List<UUID> assentos) {
  }

  @PostMapping
  public ResponseEntity<ReservaResultado> reservar(@RequestBody PedidoReserva req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(reservas.reservar(req.pedidoId(), req.compradorId(), req.eventoId(), req.assentos()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReservaResultado> buscar(@PathVariable UUID id) {
    return ResponseEntity.of(reservas.buscar(id));
  }

  @PostMapping("/{id}/confirmacao")
  public ReservaResultado confirmar(@PathVariable UUID id) {
    return reservas.confirmar(id);
  }

  @PostMapping("/{id}/liberacao")
  public ResponseEntity<Void> liberar(@PathVariable UUID id) {
    reservas.liberar(id, false);
    return ResponseEntity.noContent().build();
  }
}
