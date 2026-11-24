package br.pucrs.pulsepass.ingressos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QrEValidacaoTest {

  final AssinaturaQr assinatura = AssinaturaQr.gerarParDeChaves();

  CodigoQr codigo() {
    UUID ingresso = UUID.randomUUID();
    UUID evento = UUID.randomUUID();
    Instant emitido = Instant.ofEpochMilli(1_763_900_000_000L);
    return new CodigoQr(ingresso, evento, emitido, assinatura.assinar(CodigoQr.mensagem(ingresso, evento, emitido)));
  }

  @Test
  void textoDoQrIdaEVolta() {
    CodigoQr qr = codigo();
    assertThat(qr.texto()).startsWith("PP1.");
    assertThat(CodigoQr.ler(qr.texto())).contains(qr);
  }

  @Test
  void rejeitaTextoMalformado() {
    assertThat(CodigoQr.ler(null)).isEmpty();
    assertThat(CodigoQr.ler("qualquer coisa")).isEmpty();
    assertThat(CodigoQr.ler("XX1.a.b.c.d")).isEmpty();
    assertThat(CodigoQr.ler("PP1.nao-uuid." + UUID.randomUUID() + ".1.assin")).isEmpty();
    assertThat(CodigoQr.ler("PP1." + UUID.randomUUID() + "." + UUID.randomUUID() + ".1.")).isEmpty();
  }

  @Test
  void assinaturaValidaEAdulteracaoDetectada() {
    CodigoQr qr = codigo();
    assertThat(assinatura.verificar(qr.mensagemAssinada(), qr.assinatura())).isTrue();
    CodigoQr adulterado = new CodigoQr(UUID.randomUUID(), qr.eventoId(), qr.emitidoEm(), qr.assinatura());
    assertThat(assinatura.verificar(adulterado.mensagemAssinada(), adulterado.assinatura())).isFalse();
    assertThat(assinatura.verificar(qr.mensagemAssinada(), "###")).isFalse();
    assertThat(AssinaturaQr.gerarParDeChaves().verificar(qr.mensagemAssinada(), qr.assinatura())).isFalse();
  }

  @Test
  void carregaChavesDeBase64() throws Exception {
    KeyPair par = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    AssinaturaQr a = AssinaturaQr.deBase64(Base64.getEncoder().encodeToString(par.getPrivate().getEncoded()),
        Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
    String s = a.assinar("x");
    assertThat(a.verificar("x", s)).isTrue();
    assertThatThrownBy(() -> AssinaturaQr.deBase64("abc", "def")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void resultadoDaValidacao() {
    assertThat(ResultadoValidacao.decidir(true, true, true)).isEqualTo(ResultadoValidacao.LIBERADO);
    assertThat(ResultadoValidacao.decidir(true, true, false)).isEqualTo(ResultadoValidacao.JA_UTILIZADO);
    assertThat(ResultadoValidacao.decidir(false, true, false)).isEqualTo(ResultadoValidacao.INVALIDO);
    assertThat(ResultadoValidacao.decidir(true, false, false)).isEqualTo(ResultadoValidacao.INVALIDO);
  }

  @Test
  void estadosDoIngresso() {
    assertThat(EstadoIngresso.EMITIDO.podeValidar()).isTrue();
    assertThat(EstadoIngresso.UTILIZADO.podeValidar()).isFalse();
    assertThat(EstadoIngresso.PENDENTE.podeBaixarPdf()).isFalse();
    assertThat(EstadoIngresso.EMITIDO.podeBaixarPdf()).isTrue();
  }
}
