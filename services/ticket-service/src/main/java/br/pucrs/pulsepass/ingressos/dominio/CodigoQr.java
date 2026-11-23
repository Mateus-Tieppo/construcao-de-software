package br.pucrs.pulsepass.ingressos.dominio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Conteúdo do QR Code: {@code PP1.<ingressoId>.<eventoId>.<emitidoEmMillis>.<assinaturaBase64Url>}.
 * A assinatura cobre ingressoId + eventoId + emitidoEm (invariante I10).
 */
public record CodigoQr(UUID ingressoId, UUID eventoId, Instant emitidoEm, String assinatura) {

  public static final String PREFIXO = "PP1";

  public String mensagemAssinada() {
    return mensagem(ingressoId, eventoId, emitidoEm);
  }

  public static String mensagem(UUID ingressoId, UUID eventoId, Instant emitidoEm) {
    return ingressoId + "|" + eventoId + "|" + emitidoEm.toEpochMilli();
  }

  public String texto() {
    return PREFIXO + "." + ingressoId + "." + eventoId + "." + emitidoEm.toEpochMilli() + "." + assinatura;
  }

  public static Optional<CodigoQr> ler(String texto) {
    if (texto == null) {
      return Optional.empty();
    }
    String[] partes = texto.trim().split("\\.");
    if (partes.length != 5 || !PREFIXO.equals(partes[0]) || partes[4].isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new CodigoQr(UUID.fromString(partes[1]), UUID.fromString(partes[2]),
          Instant.ofEpochMilli(Long.parseLong(partes[3])), partes[4]));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
