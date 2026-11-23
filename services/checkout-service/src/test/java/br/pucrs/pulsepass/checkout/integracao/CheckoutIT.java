package br.pucrs.pulsepass.checkout.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.pucrs.pulsepass.checkout.adaptadores.clientes.GatewayPagamentoClient;
import br.pucrs.pulsepass.checkout.adaptadores.clientes.InventarioClient;
import br.pucrs.pulsepass.checkout.aplicacao.PedidoService;
import br.pucrs.pulsepass.checkout.dominio.RegraNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Integração do Checkout com PostgreSQL e Kafka reais (Testcontainers); Inventário e gateway simulados. */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/certs")
@AutoConfigureMockMvc
@Testcontainers
class CheckoutIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

  @Autowired
  MockMvc mvc;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  ObjectMapper mapper;

  @Autowired
  PedidoService pedidos;

  @Autowired
  TransactionTemplate tx;

  @MockBean
  InventarioClient inventario;

  @MockBean
  GatewayPagamentoClient gateway;

  final UUID comprador = UUID.randomUUID();
  final UUID evento = UUID.randomUUID();
  final UUID assento = UUID.randomUUID();

  RequestPostProcessor cliente() {
    return jwt().jwt(j -> j.subject(comprador.toString()).claim("name", "Cliente Teste"))
        .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
  }

  void inventarioReserva() {
    when(inventario.reservar(any(), eq(comprador), eq(evento), any())).thenAnswer(inv -> new InventarioClient.Reserva(
        UUID.randomUUID(), inv.getArgument(0), "ATIVA", Instant.now().plusSeconds(600), 25000,
        List.of(new InventarioClient.ItemReserva(assento, UUID.randomUUID(), 25000, "Pista A-1"))));
  }

  String corpo() {
    return "{\"eventoId\":\"" + evento + "\",\"assentos\":[\"" + assento + "\"]}";
  }

  @Test
  void semIdempotencyKeyRetorna400() throws Exception {
    mvc.perform(post("/pedidos").with(cliente()).contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("IDEMPOTENCY_KEY_AUSENTE"));
  }

  @Test
  void mesmaChaveDevolveMesmaRespostaSemNovaReserva() throws Exception {
    inventarioReserva();
    String chave = "teste-" + UUID.randomUUID();
    String r1 = mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", chave)
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("RESERVADO"))
        .andReturn().getResponse().getContentAsString();
    String r2 = mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", chave)
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "true"))
        .andReturn().getResponse().getContentAsString();
    assertThat(mapper.readTree(r2).get("id")).isEqualTo(mapper.readTree(r1).get("id"));
    verify(inventario, times(1)).reservar(any(), any(), any(), any());

    mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", chave)
            .contentType(MediaType.APPLICATION_JSON).content("{\"eventoId\":\"" + evento + "\",\"assentos\":[]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("IDEMPOTENCY_KEY_REUTILIZADA"));
  }

  @Test
  void fluxoDePagamentoChegaAPagoEGravaOutbox() throws Exception {
    inventarioReserva();
    String criado = mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", "c-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    UUID pedidoId = UUID.fromString(mapper.readTree(criado).get("id").asText());

    when(gateway.autorizar(any(), anyLong(), any())).thenAnswer(inv -> new GatewayPagamentoClient.Autorizacao(
        inv.getArgument(0), "AUTORIZADO", "SIM-1", null));
    when(inventario.confirmar(any())).thenReturn(null);

    mvc.perform(post("/pedidos/" + pedidoId + "/pagamento").with(cliente()).header("Idempotency-Key", "p-" + pedidoId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"cartaoFinal\":\"4242\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("PAGO"))
        .andExpect(jsonPath("$.ultimoPagamento").value("CAPTURADO"));
    verify(gateway).capturar(any());

    String payload = jdbc.queryForObject("""
        SELECT payload::text FROM outbox WHERE chave = ? AND tipo = 'PagamentoConfirmado'
        """, String.class, pedidoId.toString());
    JsonNode evento = mapper.readTree(payload);
    assertThat(evento.path("dados").path("itens").get(0).path("descricao").asText()).isEqualTo("Pista A-1");
    assertThat(evento.path("dados").path("compradorNome").asText()).isEqualTo("Cliente Teste");
  }

  @Test
  void pagamentoRecusadoVoltaParaReservado() throws Exception {
    inventarioReserva();
    String criado = mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", "c-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andReturn().getResponse().getContentAsString();
    UUID pedidoId = UUID.fromString(mapper.readTree(criado).get("id").asText());
    when(gateway.autorizar(any(), anyLong(), any())).thenAnswer(inv -> new GatewayPagamentoClient.Autorizacao(
        inv.getArgument(0), "RECUSADO", null, "Cartão recusado"));

    mvc.perform(post("/pedidos/" + pedidoId + "/pagamento").with(cliente()).header("Idempotency-Key", "p-" + pedidoId)
            .contentType(MediaType.APPLICATION_JSON).content("{\"cartaoFinal\":\"0002\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RESERVADO"))
        .andExpect(jsonPath("$.ultimoPagamento").value("RECUSADO"));
  }

  @Test
  void falhaNaReservaExpiraPedidoComConflito() throws Exception {
    when(inventario.reservar(any(), any(), any(), any()))
        .thenThrow(new RegraNegocioException("ASSENTO_INDISPONIVEL", "ocupado"));
    mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", "c-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("ASSENTO_INDISPONIVEL"));
    String motivo = jdbc.queryForObject("""
        SELECT motivo_expiracao FROM pedido WHERE comprador_id = ? ORDER BY criado_em DESC LIMIT 1
        """, String.class, comprador);
    assertThat(motivo).isEqualTo("FALHA_RESERVA");
  }

  @Test
  void reservaExpiradaLevaPedidoAExpirado() throws Exception {
    inventarioReserva();
    String criado = mvc.perform(post("/pedidos").with(cliente()).header("Idempotency-Key", "c-" + UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(corpo()))
        .andReturn().getResponse().getContentAsString();
    UUID pedidoId = UUID.fromString(mapper.readTree(criado).get("id").asText());

    Boolean aplicou = tx.execute(s -> pedidos.aplicarExpiracao(pedidoId));
    assertThat(aplicou).isTrue();
    assertThat(jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, pedidoId)).isEqualTo("EXPIRADO");
  }
}
