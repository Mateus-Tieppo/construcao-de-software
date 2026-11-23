package br.pucrs.pulsepass.ingressos.dominio;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Assinatura Ed25519 do QR Code (RNF22): o QR não pode ser forjado sem a chave privada. */
public final class AssinaturaQr {

  private final PrivateKey privada;
  private final PublicKey publica;

  public AssinaturaQr(PrivateKey privada, PublicKey publica) {
    this.privada = privada;
    this.publica = publica;
  }

  public static AssinaturaQr gerarParDeChaves() {
    try {
      KeyPair par = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
      return new AssinaturaQr(par.getPrivate(), par.getPublic());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Chaves em Base64 (PKCS#8 para a privada, X.509 para a pública), vindas de variáveis de ambiente. */
  public static AssinaturaQr deBase64(String privadaPkcs8, String publicaX509) {
    try {
      KeyFactory kf = KeyFactory.getInstance("Ed25519");
      return new AssinaturaQr(
          kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privadaPkcs8))),
          kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicaX509))));
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Chaves Ed25519 inválidas", e);
    }
  }

  public String assinar(String mensagem) {
    try {
      Signature s = Signature.getInstance("Ed25519");
      s.initSign(privada);
      s.update(mensagem.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(s.sign());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public boolean verificar(String mensagem, String assinaturaBase64Url) {
    try {
      Signature s = Signature.getInstance("Ed25519");
      s.initVerify(publica);
      s.update(mensagem.getBytes(StandardCharsets.UTF_8));
      return s.verify(Base64.getUrlDecoder().decode(assinaturaBase64Url));
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      return false;
    }
  }
}
