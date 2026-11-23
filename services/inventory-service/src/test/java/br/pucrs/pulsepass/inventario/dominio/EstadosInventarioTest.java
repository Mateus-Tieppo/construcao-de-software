package br.pucrs.pulsepass.inventario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class EstadosInventarioTest {

  @Test
  void transicoesDoAssento() {
    assertThat(EstadoAssento.DISPONIVEL.podeIrPara(EstadoAssento.RESERVADO)).isTrue();
    assertThat(EstadoAssento.DISPONIVEL.podeIrPara(EstadoAssento.BLOQUEADO)).isTrue();
    assertThat(EstadoAssento.DISPONIVEL.podeIrPara(EstadoAssento.VENDIDO)).isFalse();
    assertThat(EstadoAssento.RESERVADO.podeIrPara(EstadoAssento.VENDIDO)).isTrue();
    assertThat(EstadoAssento.RESERVADO.podeIrPara(EstadoAssento.DISPONIVEL)).isTrue();
    assertThat(EstadoAssento.BLOQUEADO.podeIrPara(EstadoAssento.DISPONIVEL)).isTrue();
    assertThat(EstadoAssento.BLOQUEADO.podeIrPara(EstadoAssento.RESERVADO)).isFalse();
    // invariante I5: vendido nunca volta a disponível
    for (EstadoAssento destino : EstadoAssento.values()) {
      assertThat(EstadoAssento.VENDIDO.podeIrPara(destino)).isFalse();
    }
  }

  @Test
  void publicacaoDoEvento() {
    assertThat(EstadoEvento.RASCUNHO.publicar()).isEqualTo(EstadoEvento.PUBLICADO);
    assertThat(EstadoEvento.PUBLICADO.aceitaReservas()).isTrue();
    assertThat(EstadoEvento.RASCUNHO.aceitaReservas()).isFalse();
    assertThatThrownBy(EstadoEvento.PUBLICADO::publicar).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(EstadoEvento.ENCERRADO::publicar).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void geraMapaDeAssentos() {
    List<String> rotulos = MapaAssentos.rotulos(2, 3);
    assertThat(rotulos).containsExactly("A-1", "A-2", "A-3", "B-1", "B-2", "B-3");
  }

  @Test
  void rejeitaMapaInvalido() {
    assertThatThrownBy(() -> MapaAssentos.rotulos(0, 3)).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> MapaAssentos.rotulos(27, 3)).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> MapaAssentos.rotulos(1, 0)).isInstanceOf(RegraNegocioException.class);
    assertThatThrownBy(() -> MapaAssentos.rotulos(1, 101)).isInstanceOf(RegraNegocioException.class);
  }
}
