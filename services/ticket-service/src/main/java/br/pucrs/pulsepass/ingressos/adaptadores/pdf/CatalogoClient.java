package br.pucrs.pulsepass.ingressos.adaptadores.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Busca o nome do evento no catálogo público do Inventário para impressão no PDF.
 * A projeção via tópico inventario.catalogo.v1 (grupo tkt-catalogo) está prevista para o Pacote 4.
 */
@Component
public class CatalogoClient {

  private final RestClient http;
  private final Map<UUID, String> cache = new ConcurrentHashMap<>();

  public CatalogoClient(@Value("${pulsepass.inventario.url:http://localhost:8081}") String url) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(1));
    fabrica.setReadTimeout(Duration.ofSeconds(2));
    this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
  }

  public String nomeEvento(UUID eventoId) {
    String emCache = cache.get(eventoId);
    if (emCache != null) {
      return emCache;
    }
    try {
      JsonNode evento = http.get().uri("/eventos/{id}", eventoId).retrieve().body(JsonNode.class);
      String nome = evento == null ? null : evento.path("nome").asText(null);
      if (nome != null) {
        cache.put(eventoId, nome);
      }
      return nome;
    } catch (RestClientException e) {
      return null;
    }
  }
}
