package br.pucrs.pulsepass.ingressos.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.pucrs.pulsepass.ingressos.aplicacao.ValidacaoService;
import br.pucrs.pulsepass.ingressos.dominio.ResultadoValidacao;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Consumo idempotente com Kafka e PostgreSQL reais: o mesmo PagamentoConfirmado entregue duas vezes
 * gera um único ingresso; o PDF é gravado; o QR só libera a entrada uma vez.
 */
@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/certs",
    "pulsepass.inventario.url=http://localhost:1",
    "pulsepass.pdf.intervalo-ms=200"})
@Testcontainers
class EmissaoIngressoIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  ValidacaoService validacao;

  @Test
  void pagamentoDuplicadoGeraUmIngressoEQrValeUmaVez() throws Exception {
    UUID pedido = UUID.randomUUID();
    UUID item = UUID.randomUUID();
    String evento = """
        {"eventId":"%s","tipo":"PagamentoConfirmado","schemaVersion":1,"ocorridoEm":"2026-11-23T20:00:00Z",
         "produtor":"checkout-service","agregadoId":"%s","agregadoVersao":4,
         "dados":{"pedidoId":"%s","compradorId":"%s","compradorNome":"Cliente Teste","eventoId":"%s",
                  "itens":[{"itemPedidoId":"%s","assentoId":"%s","descricao":"Pista A-1","precoCentavos":25000}]}}
        """.formatted(UUID.randomUUID(), pedido, pedido, UUID.randomUUID(), UUID.randomUUID(), item, UUID.randomUUID());

    kafkaTemplate.send("checkout.pedidos.v1", pedido.toString(), evento).get();
    kafkaTemplate.send("checkout.pedidos.v1", pedido.toString(), evento).get(); // reentrega

    String estado = null;
    long limite = System.currentTimeMillis() + 60_000;
    while (System.currentTimeMillis() < limite) {
      estado = jdbc.query("SELECT estado FROM ingresso WHERE item_pedido_id = ?",
          rs -> rs.next() ? rs.getString(1) : null, item);
      if ("EMITIDO".equals(estado)) {
        break;
      }
      Thread.sleep(300);
    }
    assertThat(estado).isEqualTo("EMITIDO");
    Thread.sleep(1500); // dá tempo de a duplicata ser consumida
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ingresso WHERE pedido_id = ?", Integer.class, pedido))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM mensagem_processada WHERE grupo = 'tkt-emissao'", Integer.class))
        .isEqualTo(1);

    String caminho = jdbc.queryForObject("SELECT caminho_pdf FROM ingresso WHERE item_pedido_id = ?", String.class, item);
    byte[] pdf = Files.readAllBytes(Path.of(caminho));
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

    String qr = jdbc.queryForObject("SELECT codigo_qr FROM ingresso WHERE item_pedido_id = ?", String.class, item);
    UUID operador = UUID.randomUUID();
    assertThat(validacao.validar(qr, operador).resultado()).isEqualTo(ResultadoValidacao.LIBERADO);
    assertThat(validacao.validar(qr, operador).resultado()).isEqualTo(ResultadoValidacao.JA_UTILIZADO);
    assertThat(validacao.validar(qr.substring(0, qr.length() - 3) + "abc", operador).resultado())
        .isEqualTo(ResultadoValidacao.INVALIDO);
  }
}
