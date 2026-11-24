package br.pucrs.pulsepass.checkout.aplicacao;

import br.pucrs.pulsepass.checkout.adaptadores.persistencia.PedidoRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Representação do pedido devolvida pela API. */
public record PedidoView(UUID id, UUID eventoId, UUID reservaId, String estado, String motivoExpiracao,
    long totalCentavos, Instant expiraEm, List<PedidoRepositorio.Item> itens, String ultimoPagamento,
    String mensagem) {
}
