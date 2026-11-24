package br.pucrs.pulsepass.simulador;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Regras do simulador: autorização em duas fases, deduplicação pela Idempotency-Key (= pagamentoId).
 * Cartão terminado em 0002 é recusado; atraso configurável para testar timeouts. Nenhum dado real de cartão.
 */
@Component
public class Simulador {

  public enum Status { AUTORIZADO, RECUSADO, CAPTURADO, CANCELADO }

  public record Transacao(UUID pagamentoId, Status status, String referencia, long valorCentavos, String mensagem) {
  }

  private final Map<UUID, Transacao> transacoes = new ConcurrentHashMap<>();
  private final long atrasoMs;

  public Simulador(@Value("${simulador.atraso-ms:0}") long atrasoMs) {
    this.atrasoMs = atrasoMs;
  }

  public Transacao autorizar(UUID pagamentoId, long valorCentavos, String cartaoFinal) {
    esperar();
    return transacoes.computeIfAbsent(pagamentoId, id -> {
      boolean recusa = cartaoFinal != null && cartaoFinal.endsWith("0002");
      if (valorCentavos <= 0 || recusa) {
        return new Transacao(id, Status.RECUSADO, "SIM-" + id.toString().substring(0, 8), valorCentavos,
            "Cartão recusado pelo emissor (simulado)");
      }
      return new Transacao(id, Status.AUTORIZADO, "SIM-" + id.toString().substring(0, 8), valorCentavos, null);
    });
  }

  public Transacao capturar(UUID pagamentoId) {
    return transacoes.compute(pagamentoId, (id, t) -> {
      if (t == null) {
        throw new IllegalArgumentException("Pagamento não autorizado: " + id);
      }
      return t.status() == Status.AUTORIZADO
          ? new Transacao(id, Status.CAPTURADO, t.referencia(), t.valorCentavos(), null) : t;
    });
  }

  public Transacao cancelar(UUID pagamentoId) {
    return transacoes.compute(pagamentoId, (id, t) -> {
      if (t == null) {
        throw new IllegalArgumentException("Pagamento inexistente: " + id);
      }
      return t.status() == Status.AUTORIZADO
          ? new Transacao(id, Status.CANCELADO, t.referencia(), t.valorCentavos(), null) : t;
    });
  }

  private void esperar() {
    if (atrasoMs > 0) {
      try {
        Thread.sleep(atrasoMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
