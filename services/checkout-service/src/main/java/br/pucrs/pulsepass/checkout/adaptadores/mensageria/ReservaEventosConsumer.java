package br.pucrs.pulsepass.checkout.adaptadores.mensageria;

import br.pucrs.pulsepass.checkout.aplicacao.PedidoService;
import br.pucrs.pulsepass.commons.inbox.InboxGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Grupo chk-reservas: ReservaExpirada leva o pedido não pago a Expirado (US19, US21). */
@Component
public class ReservaEventosConsumer {

  static final String GRUPO = "chk-reservas";

  private final ObjectMapper mapper;
  private final InboxGuard inbox;
  private final PedidoService pedidos;

  public ReservaEventosConsumer(ObjectMapper mapper, InboxGuard inbox, PedidoService pedidos) {
    this.mapper = mapper;
    this.inbox = inbox;
    this.pedidos = pedidos;
  }

  @KafkaListener(topics = "inventario.reservas.v1", groupId = GRUPO)
  @Transactional
  public void consumir(String mensagem) throws JsonProcessingException {
    JsonNode evento = mapper.readTree(mensagem);
    if (!"ReservaExpirada".equals(evento.path("tipo").asText())) {
      return;
    }
    if (!inbox.primeiraVez(GRUPO, UUID.fromString(evento.path("eventId").asText()))) {
      return;
    }
    pedidos.aplicarExpiracao(UUID.fromString(evento.path("dados").path("pedidoId").asText()));
  }
}
