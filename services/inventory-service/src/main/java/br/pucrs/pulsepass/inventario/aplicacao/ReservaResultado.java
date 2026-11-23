package br.pucrs.pulsepass.inventario.aplicacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resposta da reserva para o Checkout: itens com preço congelado (snapshot do lote). */
public record ReservaResultado(UUID reservaId, UUID pedidoId, UUID eventoId, String estado, Instant expiraEm,
    long totalCentavos, List<Item> itens) {

  public record Item(UUID assentoId, UUID loteId, long precoCentavos, String descricao) {
  }
}
