package br.pucrs.pulsepass.commons.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Catálogo de tópicos do Pacote 2 (documento 3, seção 3.2). O KafkaAdmin cria o que faltar ao subir;
 * no Compose o contêiner kafka-init também cria tudo antes dos serviços (idempotente).
 */
@Configuration
public class TopicosPulsePass {

  @Bean
  public KafkaAdmin.NewTopics topicosDominio(@Value("${pulsepass.kafka.replicas:1}") int replicas) {
    return new KafkaAdmin.NewTopics(
        topico("iam.contas.v1", 3, replicas),
        topico("inventario.catalogo.v1", 3, replicas),
        topico("inventario.reservas.v1", 6, replicas),
        topico("checkout.pedidos.v1", 6, replicas),
        topico("ingressos.v1", 3, replicas),
        topico("telemetria.navegacao.v1", 3, replicas),
        topico("iam.contas.v1.dlq", 1, replicas),
        topico("inventario.catalogo.v1.dlq", 1, replicas),
        topico("inventario.reservas.v1.dlq", 1, replicas),
        topico("checkout.pedidos.v1.dlq", 1, replicas),
        topico("ingressos.v1.dlq", 1, replicas),
        topico("telemetria.navegacao.v1.dlq", 1, replicas));
  }

  private static NewTopic topico(String nome, int particoes, int replicas) {
    return TopicBuilder.name(nome).partitions(particoes).replicas(replicas).build();
  }
}
