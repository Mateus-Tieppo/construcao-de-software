package br.pucrs.pulsepass.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PoliticaReservaTest {

  @Test
  void ordenaAssentosParaEvitarDeadlock() {
    UUID a = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID b = UUID.fromString("00000000-0000-0000-0000-000000000001");
    assertThat(PoliticaReserva.validarEOrdenar(List.of(a, b))).containsExactly(b, a);
  }

  @Test
  void rejeitaListaVaziaOuNula() {
    assertThatThrownBy(() -> PoliticaReserva.validarEOrdenar(List.of()))
        .isInstanceOf(RegraNegocioException.class).hasMessageContaining("ao menos um");
    assertThatThrownBy(() -> PoliticaReserva.validarEOrdenar(null)).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void rejeitaMaisDeSeisAssentos() {
    List<UUID> sete = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      sete.add(UUID.randomUUID());
    }
    assertThatThrownBy(() -> PoliticaReserva.validarEOrdenar(sete))
        .isInstanceOfSatisfying(RegraNegocioException.class, e -> assertThat(e.codigo()).isEqualTo("LIMITE_ASSENTOS"));
  }

  @Test
  void rejeitaAssentoRepetido() {
    UUID a = UUID.randomUUID();
    assertThatThrownBy(() -> PoliticaReserva.validarEOrdenar(List.of(a, a)))
        .isInstanceOfSatisfying(RegraNegocioException.class, e -> assertThat(e.codigo()).isEqualTo("ASSENTO_REPETIDO"));
  }

  @Test
  void expiraEmDezMinutos() {
    Instant agora = Instant.parse("2026-11-23T20:00:00Z");
    assertThat(PoliticaReserva.expiracao(agora)).isEqualTo(agora.plus(Duration.ofMinutes(10)));
  }

  @Test
  void tudoOuNada() {
    PoliticaReserva.exigirTodosBloqueados(3, 3);
    assertThatThrownBy(() -> PoliticaReserva.exigirTodosBloqueados(3, 2))
        .isInstanceOfSatisfying(RegraNegocioException.class,
            e -> assertThat(e.codigo()).isEqualTo("ASSENTO_INDISPONIVEL"));
  }

  @Test
  void confirmaSomenteAtivaDentroDoPrazo() {
    Instant expira = Instant.parse("2026-11-23T20:10:00Z");
    assertThat(PoliticaReserva.podeConfirmar(EstadoReserva.ATIVA, expira, expira.minusSeconds(1))).isTrue();
    assertThat(PoliticaReserva.podeConfirmar(EstadoReserva.ATIVA, expira, expira)).isFalse();
    assertThat(PoliticaReserva.podeConfirmar(EstadoReserva.EXPIRADA, expira, expira.minusSeconds(60))).isFalse();
  }

  @Test
  void detectaVencimento() {
    Instant expira = Instant.parse("2026-11-23T20:10:00Z");
    assertThat(PoliticaReserva.venceu(EstadoReserva.ATIVA, expira, expira)).isTrue();
    assertThat(PoliticaReserva.venceu(EstadoReserva.ATIVA, expira, expira.minusSeconds(1))).isFalse();
    assertThat(PoliticaReserva.venceu(EstadoReserva.CONFIRMADA, expira, expira.plusSeconds(1))).isFalse();
  }

  @Test
  void estadosDaReserva() {
    assertThat(EstadoReserva.ATIVA.finalizado()).isFalse();
    assertThat(EstadoReserva.CONFIRMADA.finalizado()).isTrue();
    assertThat(EstadoReserva.EXPIRADA.finalizado()).isTrue();
    assertThat(EstadoReserva.LIBERADA.finalizado()).isTrue();
  }
}
