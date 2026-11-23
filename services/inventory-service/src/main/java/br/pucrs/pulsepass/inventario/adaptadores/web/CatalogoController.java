package br.pucrs.pulsepass.inventario.adaptadores.web;

import br.pucrs.pulsepass.inventario.aplicacao.CatalogoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo público: listar eventos, detalhe e mapa de assentos (US14, US15, US17). */
@RestController
public class CatalogoController {

  private final CatalogoService catalogo;

  public CatalogoController(CatalogoService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping("/eventos")
  public List<CatalogoService.EventoResumo> listar() {
    return catalogo.listarPublicados();
  }

  @GetMapping("/eventos/{id}")
  public ResponseEntity<CatalogoService.EventoDetalhe> detalhe(@PathVariable UUID id) {
    return ResponseEntity.of(catalogo.detalhe(id));
  }

  @GetMapping("/eventos/{id}/setores/{setorId}/assentos")
  public List<CatalogoService.AssentoMapa> mapa(@PathVariable UUID id, @PathVariable UUID setorId) {
    return catalogo.mapa(id, setorId);
  }
}
