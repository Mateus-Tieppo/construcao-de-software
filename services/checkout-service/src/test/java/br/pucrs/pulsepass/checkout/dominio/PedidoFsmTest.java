package br.pucrs.pulsepass.checkout.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Cobertura da FSM do pedido (enunciado: testes unitários cobrindo máquinas de estado, >= 70%). */
class PedidoFsmTest {

  static final Instant AGORA = Instant.parse("2026-11-23T20:00:00Z");
  static final Instant EXPIRA = AGORA.plusSeconds(600);

  static Pedido novo() {
    return Pedido.novo(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  static Pedido reservado() {
    Pedido p = novo();
    p.reservar(UUID.randomUUID(), EXPIRA, 25000);
    return p;
  }

  static Pedido aguardando() {
    Pedido p = reservado();
    p.iniciarPagamento(AGORA);
    return p;
  }

  @Test
  void caminhoFelizCriadoReservadoAguardandoPago() {
    Pedido p = novo();
    assertThat(p.estado()).isEqualTo(EstadoPedido.CRIADO);
    Transicao t1 = p.reservar(UUID.randomUUID(), EXPIRA, 25000);
    assertThat(t1).isEqualTo(new Transicao(EstadoPedido.CRIADO, EstadoPedido.RESERVADO, null));
    assertThat(p.totalCentavos()).isEqualTo(25000);
    assertThat(p.expiraEm()).isEqualTo(EXPIRA);
    Transicao t2 = p.iniciarPagamento(AGORA);
    assertThat(t2.para()).isEqualTo(EstadoPedido.AGUARDANDO);
    Transicao t3 = p.confirmarPagamento();
    assertThat(t3.de()).isEqualTo(EstadoPedido.AGUARDANDO);
    assertThat(p.estado()).isEqualTo(EstadoPedido.PAGO);
    assertThat(p.motivo()).isNull();
  }

  @Test
  void reservaInvalidaERejeitada() {
    Pedido p = novo();
    assertThatThrownBy(() -> p.reservar(null, EXPIRA, 100)).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> p.reservar(UUID.randomUUID(), null, 100)).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> p.reservar(UUID.randomUUID(), EXPIRA, 0)).isInstanceOf(RegraNegocioException.class);
    assertThat(p.estado()).isEqualTo(EstadoPedido.CRIADO);
  }

  @Test
  void falhaDaReservaExpiraComMotivo() {
    Pedido p = novo();
    Transicao t = p.falharReserva();
    assertThat(t.motivo()).isEqualTo(MotivoExpiracao.FALHA_RESERVA);
    assertThat(p.estado()).isEqualTo(EstadoPedido.EXPIRADO);
  }

  @Test
  void naoIniciaPagamentoForaDoPrazo() {
    Pedido p = reservado();
    assertThatThrownBy(() -> p.iniciarPagamento(EXPIRA))
        .isInstanceOfSatisfying(RegraNegocioException.class, e -> assertThat(e.codigo()).isEqualTo("PRAZO_ENCERRADO"));
    assertThat(p.estado()).isEqualTo(EstadoPedido.RESERVADO);
  }

  @Test
  void recusaDentroDoPrazoVoltaParaReservado() {
    Pedido p = aguardando();
    Transicao t = p.recusarPagamento(AGORA.plusSeconds(10));
    assertThat(t.para()).isEqualTo(EstadoPedido.RESERVADO);
    assertThat(p.expiraEm()).isEqualTo(EXPIRA); // prazo original mantido (US24)
  }

  @Test
  void recusaForaDoPrazoExpira() {
    Pedido p = aguardando();
    Transicao t = p.recusarPagamento(EXPIRA.plusSeconds(1));
    assertThat(t.para()).isEqualTo(EstadoPedido.EXPIRADO);
    assertThat(t.motivo()).isEqualTo(MotivoExpiracao.TIMEOUT);
  }

  @Test
  void pagamentoTardioExpira() {
    Pedido p = aguardando();
    assertThat(p.pagamentoTardio().motivo()).isEqualTo(MotivoExpiracao.PAGAMENTO_TARDIO);
  }

  @Test
  void expiracaoPorTempoDeReservadoEAguardando() {
    Pedido r = reservado();
    assertThat(r.expirarPorTempo().de()).isEqualTo(EstadoPedido.RESERVADO);
    Pedido a = aguardando();
    assertThat(a.expirarPorTempo().de()).isEqualTo(EstadoPedido.AGUARDANDO);
    assertThat(a.motivo()).isEqualTo(MotivoExpiracao.TIMEOUT);
  }

  @Test
  void cancelamentoPeloComprador() {
    Pedido p = reservado();
    assertThat(p.cancelar().motivo()).isEqualTo(MotivoExpiracao.CANCELADO_USUARIO);
  }

  @Test
  void pagoEFinalENaoExpira() {
    Pedido p = aguardando();
    p.confirmarPagamento();
    // invariante I7: Expirado nunca vira Pago e Pago nunca expira
    assertThatThrownBy(p::expirarPorTempo).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::cancelar).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::confirmarPagamento).isInstanceOf(TransicaoInvalidaException.class);
  }

  @Test
  void expiradoEFinal() {
    Pedido p = reservado();
    p.cancelar();
    assertThatThrownBy(() -> p.iniciarPagamento(AGORA)).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::expirarPorTempo).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::falharReserva).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::pagamentoTardio).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(() -> p.recusarPagamento(AGORA)).isInstanceOf(TransicaoInvalidaException.class);
  }

  @Test
  void criadoNaoPagaNemCancela() {
    Pedido p = novo();
    assertThatThrownBy(() -> p.iniciarPagamento(AGORA)).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::cancelar).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(p::expirarPorTempo).isInstanceOf(TransicaoInvalidaException.class);
  }

  @Test
  void reservadoNaoConfirmaDireto() {
    Pedido p = reservado();
    assertThatThrownBy(p::confirmarPagamento).isInstanceOf(TransicaoInvalidaException.class);
    assertThatThrownBy(() -> p.reservar(UUID.randomUUID(), EXPIRA, 1)).isInstanceOf(TransicaoInvalidaException.class);
  }

  @ParameterizedTest
  @EnumSource(EstadoPedido.class)
  void estadosFinaisNaoTemSaida(EstadoPedido estado) {
    assertThat(estado.finalizado()).isEqualTo(estado.destinosPermitidos().isEmpty());
  }

  @Test
  void tabelaDeTransicoes() {
    assertThat(EstadoPedido.CRIADO.podeIrPara(EstadoPedido.RESERVADO)).isTrue();
    assertThat(EstadoPedido.CRIADO.podeIrPara(EstadoPedido.PAGO)).isFalse();
    assertThat(EstadoPedido.RESERVADO.podeIrPara(EstadoPedido.PAGO)).isFalse();
    assertThat(EstadoPedido.AGUARDANDO.podeIrPara(EstadoPedido.RESERVADO)).isTrue();
    assertThat(EstadoPedido.EXPIRADO.podeIrPara(EstadoPedido.PAGO)).isFalse();
  }

  @Test
  void donoDoPedido() {
    UUID comprador = UUID.randomUUID();
    Pedido p = Pedido.novo(UUID.randomUUID(), comprador, UUID.randomUUID());
    assertThat(p.pertenceA(comprador)).isTrue();
    assertThat(p.pertenceA(UUID.randomUUID())).isFalse();
    assertThat(p.versao()).isEqualTo(1);
    assertThat(p.reservaId()).isNull();
    assertThat(p.compradorId()).isEqualTo(comprador);
    assertThat(p.eventoId()).isNotNull();
    assertThat(p.id()).isNotNull();
  }
}
