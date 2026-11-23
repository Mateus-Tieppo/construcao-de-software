package br.pucrs.pulsepass.inventario.adaptadores.web;

import br.pucrs.pulsepass.inventario.aplicacao.EventoOrganizadorService;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints do organizador (papel ORGANIZADOR, só sobre os próprios eventos). */
@RestController
@RequestMapping("/organizador/eventos")
public class OrganizadorEventoController {

  private final EventoOrganizadorService eventos;

  public OrganizadorEventoController(EventoOrganizadorService eventos) {
    this.eventos = eventos;
  }

  @PostMapping
  public ResponseEntity<Map<String, UUID>> criar(@AuthenticationPrincipal Jwt jwt,
      @RequestBody EventoOrganizadorService.NovoEvento novo) {
    UUID id = eventos.criar(UUID.fromString(jwt.getSubject()), novo);
    return ResponseEntity.created(URI.create("/eventos/" + id)).body(Map.of("id", id));
  }

  @PostMapping("/{id}/publicacao")
  public ResponseEntity<Void> publicar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    eventos.publicar(UUID.fromString(jwt.getSubject()), id);
    return ResponseEntity.noContent().build();
  }
}
