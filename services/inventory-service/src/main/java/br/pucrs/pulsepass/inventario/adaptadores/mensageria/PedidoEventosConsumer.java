package br.pucrs.pulsepass.inventario.adaptadores.mensageria;

import br.pucrs.pulsepass.commons.inbox.InboxGuard;
import br.pucrs.pulsepass.inventario.aplicacao.ReservaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grupo inv-pedidos: PedidoExpirado libera a reserva do pedido (rede de segurança quando a chamada
 * síncrona do Checkout falhou). Inbox + efeito na mesma transação (RNF07).
 */
@Component
public class PedidoEventosConsumer {

  static final String GRUPO = "inv-pedidos";

  private final ObjectMapper mapper;
  private final InboxGuard inbox;
  private final ReservaService reservas;

  public PedidoEventosConsumer(ObjectMapper mapper, InboxGuard inbox, ReservaService reservas) {
    this.mapper = mapper;
    this.inbox = inbox;
    this.reservas = reservas;
  }

  @KafkaListener(topics = "checkout.pedidos.v1", groupId = GRUPO)
  @Transactional
  public void consumir(String mensagem) throws JsonProcessingException {
    JsonNode evento = mapper.readTree(mensagem);
    if (!"PedidoExpirado".equals(evento.path("tipo").asText())) {
      return;
    }
    UUID eventId = UUID.fromString(evento.path("eventId").asText());
    if (!inbox.primeiraVez(GRUPO, eventId)) {
      return;
    }
    reservas.liberarPorPedido(UUID.fromString(evento.path("dados").path("pedidoId").asText()));
  }
}
