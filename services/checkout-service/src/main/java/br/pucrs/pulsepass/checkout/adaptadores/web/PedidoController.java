package br.pucrs.pulsepass.checkout.adaptadores.web;

import br.pucrs.pulsepass.checkout.aplicacao.PedidoService;
import br.pucrs.pulsepass.checkout.aplicacao.PedidoView;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de pedidos (US21, US22). Os POST passam pelo filtro de Idempotency-Key. */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

  private final PedidoService pedidos;

  public PedidoController(PedidoService pedidos) {
    this.pedidos = pedidos;
  }

  public record NovoPedido(UUID eventoId, List<UUID> assentos) {
  }

  public record DadosPagamento(String cartaoFinal) {
  }

  @PostMapping
  public ResponseEntity<PedidoView> criar(@AuthenticationPrincipal Jwt jwt, @RequestBody NovoPedido req) {
    PedidoView v = pedidos.criar(usuario(jwt), nome(jwt), req.eventoId(), req.assentos());
    return ResponseEntity.created(URI.create("/pedidos/" + v.id())).body(v);
  }

  @PostMapping("/{id}/pagamento")
  public PedidoView pagar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
      @RequestBody(required = false) DadosPagamento req) {
    return pedidos.pagar(id, usuario(jwt), req == null ? null : req.cartaoFinal());
  }

  @PostMapping("/{id}/cancelamento")
  public PedidoView cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return pedidos.cancelar(id, usuario(jwt));
  }

  @GetMapping("/{id}")
  public PedidoView consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    return pedidos.consultar(id, usuario(jwt));
  }

  @GetMapping
  public List<PedidoView> meus(@AuthenticationPrincipal Jwt jwt) {
    return pedidos.meus(usuario(jwt));
  }

  static UUID usuario(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }

  static String nome(Jwt jwt) {
    String nome = jwt.getClaimAsString("name");
    return nome != null ? nome : jwt.getClaimAsString("preferred_username");
  }
}
