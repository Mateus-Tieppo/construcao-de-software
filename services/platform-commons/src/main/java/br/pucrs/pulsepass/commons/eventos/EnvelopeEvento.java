package br.pucrs.pulsepass.commons.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Envelope comum de todos os eventos de domínio (Pacote 2, documento 3, seção 3.3; ADR-005).
 */
public record EnvelopeEvento(
    UUID eventId,
    String tipo,
    int schemaVersion,
    Instant ocorridoEm,
    String produtor,
    UUID agregadoId,
    long agregadoVersao,
    UUID correlationId,
    UUID causationId,
    JsonNode dados) {
}
