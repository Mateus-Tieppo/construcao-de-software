package br.pucrs.pulsepass.checkout.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Regras da Idempotency-Key (RNF05, Pacote 2 seção 1.5.4): chave obrigatória, formato limitado e
 * hash SHA-256 de método + rota + corpo para detectar reutilização da chave com outra requisição.
 */
public final class RegraIdempotencia {

  public static final int TAMANHO_MAXIMO = 100;

  private RegraIdempotencia() {
  }

  public static void validarChave(String chave) {
    if (chave == null || chave.isBlank()) {
      throw new RegraNegocioException("IDEMPOTENCY_KEY_AUSENTE", "Cabeçalho Idempotency-Key é obrigatório");
    }
    if (chave.length() > TAMANHO_MAXIMO || !chave.matches("[A-Za-z0-9_\\-:.]+")) {
      throw new RegraNegocioException("IDEMPOTENCY_KEY_INVALIDA", "Idempotency-Key inválida");
    }
  }

  public static String hash(String metodo, String rota, byte[] corpo) {
    try {
      MessageDigest sha = MessageDigest.getInstance("SHA-256");
      sha.update(metodo.getBytes(StandardCharsets.UTF_8));
      sha.update((byte) '\n');
      sha.update(rota.getBytes(StandardCharsets.UTF_8));
      sha.update((byte) '\n');
      sha.update(corpo == null ? new byte[0] : corpo);
      return HexFormat.of().formatHex(sha.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Decisão para uma chave já registrada. */
  public enum Decisao { REPETIR_RESPOSTA, EM_ANDAMENTO, CORPO_DIFERENTE }

  public static Decisao decidir(String hashRegistrado, String hashAtual, boolean concluido) {
    if (!hashRegistrado.equals(hashAtual)) {
      return Decisao.CORPO_DIFERENTE;
    }
    return concluido ? Decisao.REPETIR_RESPOSTA : Decisao.EM_ANDAMENTO;
  }
}
