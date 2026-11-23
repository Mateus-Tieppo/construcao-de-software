package br.pucrs.pulsepass.commons.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Retentativas e DLQ dos consumidores (Pacote 2, documento 3, seção 3.8):
 * 3 tentativas com backoff 1 s, 2 s, 4 s; erro permanente vai direto para {@code <tópico>.dlq}.
 */
@Configuration
public class ConfiguracaoKafkaConsumo {

  @Bean
  public DefaultErrorHandler tratadorErrosKafka(KafkaTemplate<String, String> kafka) {
    DeadLetterPublishingRecoverer dlq = new DeadLetterPublishingRecoverer(kafka,
        (registro, erro) -> new TopicPartition(registro.topic() + ".dlq", 0));
    ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
    backoff.setMaxAttempts(3);
    DefaultErrorHandler handler = new DefaultErrorHandler(dlq, backoff);
    handler.addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class);
    return handler;
  }
}
