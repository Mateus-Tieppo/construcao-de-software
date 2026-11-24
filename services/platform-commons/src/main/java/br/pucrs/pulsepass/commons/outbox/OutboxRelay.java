package br.pucrs.pulsepass.commons.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica no Kafka os registros pendentes da outbox, em ordem de id (ADR-005).
 * Só uma réplica publica por vez: pg_try_advisory_xact_lock com o id do serviço.
 * Entrega pelo menos uma vez: se cair depois do ack e antes do UPDATE, o mesmo eventId é reenviado
 * e os consumidores descartam pela Inbox.
 */
@Component
public class OutboxRelay {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, String> kafka;
  private final TransactionTemplate tx;
  private final long lockId;

  public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx,
      @Value("${pulsepass.outbox.lock-id}") long lockId) {
    this.jdbc = jdbc;
    this.kafka = kafka;
    this.tx = tx;
    this.lockId = lockId;
  }

  @Scheduled(fixedDelayString = "${pulsepass.outbox.intervalo-ms:300}")
  public void publicarPendentes() {
    try {
      Integer publicados = tx.execute(status -> publicarLote());
      if (publicados != null && publicados > 0) {
        LOG.debug("Outbox: {} eventos publicados", publicados);
      }
    } catch (RuntimeException e) {
      LOG.warn("Outbox: falha ao publicar, nova tentativa no próximo ciclo: {}", e.getMessage());
    }
  }

  int publicarLote() {
    Boolean lider = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lockId);
    if (!Boolean.TRUE.equals(lider)) {
      return 0;
    }
    List<RegistroOutbox> pendentes = jdbc.query("""
        SELECT id, event_id, topico, chave, tipo, payload::text AS payload
          FROM outbox WHERE publicado_em IS NULL ORDER BY id LIMIT 100
        """, (rs, n) -> new RegistroOutbox(rs.getLong("id"), rs.getString("event_id"), rs.getString("topico"),
        rs.getString("chave"), rs.getString("tipo"), rs.getString("payload")));
    int ok = 0;
    for (RegistroOutbox r : pendentes) {
      try {
        kafka.send(MessageBuilder.withPayload(r.payload())
            .setHeader(KafkaHeaders.TOPIC, r.topico())
            .setHeader(KafkaHeaders.KEY, r.chave())
            .setHeader("eventId", r.eventId())
            .setHeader("tipo", r.tipo())
            .build()).get(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        // para no primeiro erro para não deixar eventos posteriores passarem na frente
        jdbc.update("UPDATE outbox SET tentativas = tentativas + 1 WHERE id = ?", r.id());
        LOG.warn("Outbox: envio do registro {} falhou: {}", r.id(), e.getMessage());
        break;
      }
      jdbc.update("UPDATE outbox SET publicado_em = now() WHERE id = ?", r.id());
      ok++;
    }
    return ok;
  }

  record RegistroOutbox(long id, String eventId, String topico, String chave, String tipo, String payload) {
  }
}
