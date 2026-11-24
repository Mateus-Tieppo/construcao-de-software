package br.pucrs.pulsepass.commons.outbox;

import br.pucrs.pulsepass.commons.eventos.EnvelopeEvento;
import br.pucrs.pulsepass.commons.eventos.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o evento na tabela outbox na MESMA transação da mudança de estado (Transactional Outbox, RNF06).
 * Exige uma transação aberta pelo caso de uso: assim estado e evento são gravados juntos ou nenhum.
 */
@Component
public class OutboxWriter {

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final String produtor;

  public OutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper,
      @Value("${spring.application.name}") String produtor) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.produtor = produtor;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public UUID gravar(String topico, String tipo, UUID agregadoId, long agregadoVersao,
      UUID correlationId, UUID causationId, Object dados) {
    UUID eventId = Uuids.v7();
    Instant agora = Instant.now();
    JsonNode dadosJson = mapper.valueToTree(dados);
    EnvelopeEvento envelope = new EnvelopeEvento(eventId, tipo, 1, agora, produtor, agregadoId,
        agregadoVersao, correlationId, causationId, dadosJson);
    String payload;
    try {
      payload = mapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar evento " + tipo, e);
    }
    jdbc.update("""
        INSERT INTO outbox (event_id, topico, chave, tipo, payload, criado_em)
        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?)
        """, eventId, topico, agregadoId.toString(), tipo, payload, Timestamp.from(agora));
    return eventId;
  }
}
