package br.pucrs.pulsepass.ingressos.adaptadores.mensageria;

import br.pucrs.pulsepass.ingressos.aplicacao.EmissaoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Grupo tkt-emissao: consome PagamentoConfirmado de checkout.pedidos.v1 e registra a emissão. */
@Component
public class PagamentoConfirmadoConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(PagamentoConfirmadoConsumer.class);

  private final ObjectMapper mapper;
  private final EmissaoService emissao;

  public PagamentoConfirmadoConsumer(ObjectMapper mapper, EmissaoService emissao) {
    this.mapper = mapper;
    this.emissao = emissao;
  }

  @KafkaListener(topics = "checkout.pedidos.v1", groupId = EmissaoService.GRUPO)
  public void consumir(String mensagem) throws JsonProcessingException {
    JsonNode evento = mapper.readTree(mensagem);
    if (!"PagamentoConfirmado".equals(evento.path("tipo").asText())) {
      return;
    }
    int criados = emissao.registrar(evento);
    LOG.info("PagamentoConfirmado {}: {} ingressos registrados", evento.path("eventId").asText(), criados);
  }
}
