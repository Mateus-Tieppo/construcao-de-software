package br.pucrs.pulsepass.ingressos.aplicacao;

import br.pucrs.pulsepass.ingressos.dominio.AssinaturaQr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chaves Ed25519 do QR vêm de variáveis de ambiente (nunca do Git). Sem elas, gera um par efêmero
 * só para desenvolvimento: QR emitidos antes de um reinício deixam de validar.
 */
@Configuration
public class ConfiguracaoAssinatura {

  private static final Logger LOG = LoggerFactory.getLogger(ConfiguracaoAssinatura.class);

  @Bean
  public AssinaturaQr assinaturaQr(@Value("${pulsepass.qr.chave-privada:}") String privada,
      @Value("${pulsepass.qr.chave-publica:}") String publica) {
    if (privada.isBlank() || publica.isBlank()) {
      LOG.warn("QR_CHAVE_PRIVADA/QR_CHAVE_PUBLICA não definidas: usando par de chaves efêmero (apenas desenvolvimento)");
      return AssinaturaQr.gerarParDeChaves();
    }
    return AssinaturaQr.deBase64(privada, publica);
  }
}
