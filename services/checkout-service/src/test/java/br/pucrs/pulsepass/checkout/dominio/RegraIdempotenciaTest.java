package br.pucrs.pulsepass.checkout.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RegraIdempotenciaTest {

  @Test
  void chaveObrigatoriaEValida() {
    RegraIdempotencia.validarChave("pedido-123_abc:1.0");
    assertThatThrownBy(() -> RegraIdempotencia.validarChave(null))
        .isInstanceOfSatisfying(RegraNegocioException.class,
            e -> assertThat(e.codigo()).isEqualTo("IDEMPOTENCY_KEY_AUSENTE"));
    assertThatThrownBy(() -> RegraIdempotencia.validarChave("  ")).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> RegraIdempotencia.validarChave("com espaço"))
        .isInstanceOfSatisfying(RegraNegocioException.class,
            e -> assertThat(e.codigo()).isEqualTo("IDEMPOTENCY_KEY_INVALIDA"));
    assertThatThrownBy(() -> RegraIdempotencia.validarChave("x".repeat(101))).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void hashDependeDeMetodoRotaECorpo() {
    byte[] corpo = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
    String h = RegraIdempotencia.hash("POST", "/pedidos", corpo);
    assertThat(h).hasSize(64).isEqualTo(RegraIdempotencia.hash("POST", "/pedidos", corpo));
    assertThat(h).isNotEqualTo(RegraIdempotencia.hash("POST", "/pedidos/1/pagamento", corpo));
    assertThat(h).isNotEqualTo(RegraIdempotencia.hash("POST", "/pedidos", "{\"a\":2}".getBytes(StandardCharsets.UTF_8)));
    assertThat(RegraIdempotencia.hash("POST", "/pedidos", null)).hasSize(64);
  }

  @Test
  void decisaoParaChaveRepetida() {
    assertThat(RegraIdempotencia.decidir("a", "a", true)).isEqualTo(RegraIdempotencia.Decisao.REPETIR_RESPOSTA);
    assertThat(RegraIdempotencia.decidir("a", "a", false)).isEqualTo(RegraIdempotencia.Decisao.EM_ANDAMENTO);
    assertThat(RegraIdempotencia.decidir("a", "b", true)).isEqualTo(RegraIdempotencia.Decisao.CORPO_DIFERENTE);
  }
}
