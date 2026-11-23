package br.pucrs.pulsepass.checkout.adaptadores.persistencia;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Remove registros de Idempotency-Key vencidos (retenção de 24 h). */
@Component
public class LimpezaIdempotencia {

  private final IdempotenciaRepositorio repo;

  public LimpezaIdempotencia(IdempotenciaRepositorio repo) {
    this.repo = repo;
  }

  @Scheduled(fixedDelay = 3_600_000L, initialDelay = 60_000L)
  public void limpar() {
    repo.limparVencidos();
  }
}
