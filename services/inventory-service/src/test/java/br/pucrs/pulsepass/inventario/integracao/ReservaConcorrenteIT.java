package br.pucrs.pulsepass.inventario.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.pucrs.pulsepass.commons.outbox.OutboxRelay;
import br.pucrs.pulsepass.inventario.aplicacao.ExpiracaoWorker;
import br.pucrs.pulsepass.inventario.aplicacao.ReservaResultado;
import br.pucrs.pulsepass.inventario.aplicacao.ReservaService;
import br.pucrs.pulsepass.inventario.dominio.RegraNegocioException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integração com Testcontainers reais de PostgreSQL e Kafka (enunciado).
 * Prova zero overselling sob concorrência, expiração e publicação via Outbox.
 */
@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/certs",
    "pulsepass.expiracao.intervalo-ms=600000"})
@Testcontainers
class ReservaConcorrenteIT {

  static final UUID EVENTO = UUID.fromString("0193e0aa-0000-7000-8000-000000000001");

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Autowired
  ReservaService reservas;

  @Autowired
  ExpiracaoWorker expiracao;

  @Autowired
  OutboxRelay relay;

  @Autowired
  JdbcTemplate jdbc;

  UUID assento(String setor, String rotulo) {
    return jdbc.queryForObject("""
        SELECT a.id FROM assento a JOIN setor s ON s.id = a.setor_id WHERE s.nome = ? AND a.fila_numero = ?
        """, UUID.class, setor, rotulo);
  }

  @Test
  void cemCompradoresDisputandoOMesmoAssentoSoUmVence() throws Exception {
    UUID alvo = assento("Pista", "A-1");
    int compradores = 100;
    ExecutorService pool = Executors.newFixedThreadPool(32);
    AtomicInteger vencedores = new AtomicInteger();
    AtomicInteger recusados = new AtomicInteger();
    List<Future<?>> futuros = new ArrayList<>();
    for (int i = 0; i < compradores; i++) {
      Callable<Void> tarefa = () -> {
        try {
          reservas.reservar(UUID.randomUUID(), UUID.randomUUID(), EVENTO, List.of(alvo));
          vencedores.incrementAndGet();
        } catch (RegraNegocioException e) {
          recusados.incrementAndGet();
        }
        return null;
      };
      futuros.add(pool.submit(tarefa));
    }
    for (Future<?> f : futuros) {
      f.get();
    }
    pool.shutdown();

    assertThat(vencedores.get()).isEqualTo(1);
    assertThat(recusados.get()).isEqualTo(compradores - 1);
    Integer reservasDoAssento = jdbc.queryForObject("SELECT count(*) FROM item_reserva WHERE assento_id = ?",
        Integer.class, alvo);
    assertThat(reservasDoAssento).isEqualTo(1);
  }

  @Test
  void reservaComVariosAssentosETudoOuNada() {
    UUID livre = assento("Pista", "B-1");
    UUID ocupado = assento("Pista", "B-2");
    reservas.reservar(UUID.randomUUID(), UUID.randomUUID(), EVENTO, List.of(ocupado));

    UUID pedido = UUID.randomUUID();
    assertThatThrownBy(() -> reservas.reservar(pedido, UUID.randomUUID(), EVENTO, List.of(livre, ocupado)))
        .isInstanceOfSatisfying(RegraNegocioException.class,
            e -> assertThat(e.codigo()).isEqualTo("ASSENTO_INDISPONIVEL"));
    String estadoLivre = jdbc.queryForObject("SELECT estado FROM assento WHERE id = ?", String.class, livre);
    assertThat(estadoLivre).isEqualTo("DISPONIVEL");
    assertThat(reservas.buscarPorPedido(pedido)).isEmpty();
  }

  @Test
  void reservaEIdempotentePorPedido() {
    UUID pedido = UUID.randomUUID();
    UUID a = assento("Camarote", "A-1");
    ReservaResultado r1 = reservas.reservar(pedido, UUID.randomUUID(), EVENTO, List.of(a));
    ReservaResultado r2 = reservas.reservar(pedido, UUID.randomUUID(), EVENTO, List.of(a));
    assertThat(r2.reservaId()).isEqualTo(r1.reservaId());
    assertThat(r1.totalCentavos()).isEqualTo(60000);
  }

  @Test
  void confirmacaoVendeAssentoEReservaVencidaNaoConfirma() {
    UUID a = assento("Camarote", "B-1");
    ReservaResultado r = reservas.reservar(UUID.randomUUID(), UUID.randomUUID(), EVENTO, List.of(a));
    ReservaResultado confirmada = reservas.confirmar(r.reservaId());
    assertThat(confirmada.estado()).isEqualTo("CONFIRMADA");
    assertThat(jdbc.queryForObject("SELECT estado FROM assento WHERE id = ?", String.class, a)).isEqualTo("VENDIDO");

    UUID b = assento("Camarote", "B-2");
    ReservaResultado vencida = reservas.reservar(UUID.randomUUID(), UUID.randomUUID(), EVENTO, List.of(b));
    jdbc.update("UPDATE reserva SET expira_em = now() - interval '1 second' WHERE id = ?", vencida.reservaId());
    assertThatThrownBy(() -> reservas.confirmar(vencida.reservaId()))
        .isInstanceOfSatisfying(RegraNegocioException.class, e -> assertThat(e.codigo()).isEqualTo("RESERVA_EXPIRADA"));
    assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, vencida.reservaId()))
        .isEqualTo("ATIVA");
  }

  @Test
  void expiracaoDevolveAssentosEPublicaNoKafkaPelaOutbox() throws InterruptedException {
    UUID a = assento("Pista", "C-1");
    ReservaResultado r = reservas.reservar(UUID.randomUUID(), UUID.randomUUID(), EVENTO, List.of(a));
    jdbc.update("UPDATE reserva SET expira_em = now() - interval '1 second' WHERE id = ?", r.reservaId());

    assertThat(expiracao.expirarVencidas()).isGreaterThanOrEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT estado FROM assento WHERE id = ?", String.class, a)).isEqualTo("DISPONIVEL");
    assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, r.reservaId()))
        .isEqualTo("EXPIRADA");

    // o relay agendado também roda em paralelo e pode estar com o advisory lock: espera esvaziar a outbox
    Integer pendentes = -1;
    long prazo = System.currentTimeMillis() + 15000;
    while (System.currentTimeMillis() < prazo) {
      relay.publicarPendentes();
      pendentes = jdbc.queryForObject("SELECT count(*) FROM outbox WHERE publicado_em IS NULL", Integer.class);
      if (pendentes != null && pendentes == 0) {
        break;
      }
      Thread.sleep(200);
    }
    assertThat(pendentes).isZero();

    boolean achou = false;
    try (KafkaConsumer<String, String> consumer = novoConsumidor()) {
      consumer.subscribe(List.of("inventario.reservas.v1"));
      long limite = System.currentTimeMillis() + 15000;
      while (!achou && System.currentTimeMillis() < limite) {
        for (ConsumerRecord<String, String> rec : consumer.poll(Duration.ofMillis(500))) {
          if (rec.key().equals(r.reservaId().toString()) && rec.value().contains("\"ReservaExpirada\"")) {
            achou = true;
          }
        }
      }
    }
    assertThat(achou).as("evento ReservaExpirada publicado no Kafka").isTrue();
  }

  private KafkaConsumer<String, String> novoConsumidor() {
    Properties p = new Properties();
    p.putAll(Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
        ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()));
    return new KafkaConsumer<>(p);
  }
}
