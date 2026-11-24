package br.pucrs.pulsepass.ingressos.dominio;

/** Resultado da leitura do QR na entrada (US29). */
public enum ResultadoValidacao {
  LIBERADO, JA_UTILIZADO, INVALIDO, NAO_AUTORIZADO;

  /** Decide o resultado a partir da assinatura e do UPDATE condicional de uso único (invariante I9). */
  public static ResultadoValidacao decidir(boolean assinaturaValida, boolean ingressoExiste, boolean marcouUso) {
    if (!assinaturaValida || !ingressoExiste) {
      return INVALIDO;
    }
    return marcouUso ? LIBERADO : JA_UTILIZADO;
  }
}
