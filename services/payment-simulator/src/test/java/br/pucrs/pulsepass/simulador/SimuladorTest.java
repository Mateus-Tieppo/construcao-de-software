package br.pucrs.pulsepass.simulador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SimuladorTest {

  final Simulador simulador = new Simulador(0);

  @Test
  void autorizaECapturaUmaVez() {
    UUID id = UUID.randomUUID();
    assertThat(simulador.autorizar(id, 1000, "4242").status()).isEqualTo(Simulador.Status.AUTORIZADO);
    // mesma Idempotency-Key: não gera nova autorização, mesmo com outro cartão
    assertThat(simulador.autorizar(id, 1000, "0002").status()).isEqualTo(Simulador.Status.AUTORIZADO);
    assertThat(simulador.capturar(id).status()).isEqualTo(Simulador.Status.CAPTURADO);
    assertThat(simulador.cancelar(id).status()).isEqualTo(Simulador.Status.CAPTURADO);
  }

  @Test
  void recusaCartaoDeTesteECancela() {
    assertThat(simulador.autorizar(UUID.randomUUID(), 1000, "0002").status()).isEqualTo(Simulador.Status.RECUSADO);
    UUID id = UUID.randomUUID();
    simulador.autorizar(id, 1000, null);
    assertThat(simulador.cancelar(id).status()).isEqualTo(Simulador.Status.CANCELADO);
    assertThatThrownBy(() -> simulador.capturar(UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
  }
}
