package br.pucrs.pulsepass.checkout.adaptadores.clientes;

import br.pucrs.pulsepass.checkout.dominio.RegraNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente REST interno do Inventory Service (timeout de 2 s, Pacote 2 Figura 5). */
@Component
public class InventarioClient {

  private final RestClient http;
  private final ObjectMapper mapper;

  public InventarioClient(@Value("${pulsepass.inventario.url}") String url, ObjectMapper mapper) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(1));
    fabrica.setReadTimeout(Duration.ofSeconds(2));
    this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    this.mapper = mapper;
  }

  public record ItemReserva(UUID assentoId, UUID loteId, long precoCentavos, String descricao) {
  }

  public record Reserva(UUID reservaId, UUID pedidoId, String estado, Instant expiraEm, long totalCentavos,
      List<ItemReserva> itens) {
  }

  record PedidoReserva(UUID pedidoId, UUID compradorId, UUID eventoId, List<UUID> assentos) {
  }

  public Reserva reservar(UUID pedidoId, UUID compradorId, UUID eventoId, List<UUID> assentos) {
    return chamar(() -> http.post().uri("/internal/reservas")
        .body(new PedidoReserva(pedidoId, compradorId, eventoId, assentos)).retrieve().body(Reserva.class));
  }

  public Reserva confirmar(UUID reservaId) {
    return chamar(() -> http.post().uri("/internal/reservas/{id}/confirmacao", reservaId).retrieve().body(Reserva.class));
  }

  public void liberar(UUID reservaId) {
    chamar(() -> http.post().uri("/internal/reservas/{id}/liberacao", reservaId).retrieve().toBodilessEntity());
  }

  private <T> T chamar(java.util.function.Supplier<T> chamada) {
    try {
      return chamada.get();
    } catch (HttpClientErrorException e) {
      String codigo = "INVENTARIO_RECUSOU";
      try {
        JsonNode corpo = mapper.readTree(e.getResponseBodyAsString());
        codigo = corpo.path("codigo").asText(codigo);
      } catch (java.io.IOException ignorado) {
        codigo = "INVENTARIO_RECUSOU";
      }
      throw new RegraNegocioException(codigo, "Inventário recusou: " + codigo);
    } catch (RestClientException e) {
      throw new ServicoIndisponivelException("Inventário indisponível", e);
    }
  }
}
