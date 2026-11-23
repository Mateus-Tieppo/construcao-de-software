package br.pucrs.pulsepass.checkout.adaptadores.clientes;

import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente do simulador de gateway financeiro (duas fases: autorizar e capturar, ADR-003).
 * Idempotency-Key = pagamentoId: repetir a chamada nunca gera nova cobrança.
 */
@Component
public class GatewayPagamentoClient {

  private final RestClient http;

  public GatewayPagamentoClient(@Value("${pulsepass.pagamento.url}") String url) {
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofSeconds(1));
    fabrica.setReadTimeout(Duration.ofSeconds(3));
    this.http = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
  }

  public record Autorizacao(UUID pagamentoId, String status, String referencia, String mensagem) {
    public boolean autorizado() {
      return "AUTORIZADO".equals(status);
    }
  }

  record PedidoAutorizacao(UUID pagamentoId, long valorCentavos, String cartaoFinal) {
  }

  public Autorizacao autorizar(UUID pagamentoId, long valorCentavos, String cartaoFinal) {
    try {
      return http.post().uri("/pagamentos/autorizacoes").header("Idempotency-Key", pagamentoId.toString())
          .body(new PedidoAutorizacao(pagamentoId, valorCentavos, cartaoFinal)).retrieve().body(Autorizacao.class);
    } catch (RestClientException e) {
      throw new ServicoIndisponivelException("Gateway de pagamento indisponível", e);
    }
  }

  public void capturar(UUID pagamentoId) {
    try {
      http.post().uri("/pagamentos/{id}/captura", pagamentoId).header("Idempotency-Key", pagamentoId + "-captura")
          .retrieve().toBodilessEntity();
    } catch (RestClientException e) {
      throw new ServicoIndisponivelException("Gateway de pagamento indisponível", e);
    }
  }

  public void cancelar(UUID pagamentoId) {
    try {
      http.post().uri("/pagamentos/{id}/cancelamento", pagamentoId).header("Idempotency-Key", pagamentoId + "-cancel")
          .retrieve().toBodilessEntity();
    } catch (RestClientException e) {
      throw new ServicoIndisponivelException("Gateway de pagamento indisponível", e);
    }
  }
}
