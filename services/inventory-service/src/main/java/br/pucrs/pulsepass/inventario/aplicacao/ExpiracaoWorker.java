package br.pucrs.pulsepass.inventario.aplicacao;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Varre reservas vencidas a cada segundo (US19, expiração estrita de 10 min).
 * FOR UPDATE SKIP LOCKED permite várias réplicas sem liberar a mesma reserva duas vezes.
 */
@Component
public class ExpiracaoWorker {

  private static final Logger LOG = LoggerFactory.getLogger(ExpiracaoWorker.class);

  private final JdbcTemplate jdbc;
  private final ReservaService reservas;

  public ExpiracaoWorker(JdbcTemplate jdbc, ReservaService reservas) {
    this.jdbc = jdbc;
    this.reservas = reservas;
  }

  @Scheduled(fixedDelayString = "${pulsepass.expiracao.intervalo-ms:1000}")
  @Transactional
  public int expirarVencidas() {
    List<UUID> vencidas = jdbc.query("""
        SELECT id FROM reserva WHERE estado = 'ATIVA' AND expira_em <= now()
         ORDER BY expira_em LIMIT 50 FOR UPDATE SKIP LOCKED
        """, (rs, n) -> rs.getObject(1, UUID.class));
    int liberadas = 0;
    for (UUID id : vencidas) {
      if (reservas.liberar(id, true)) {
        liberadas++;
      }
    }
    if (liberadas > 0) {
      LOG.info("Expiração: {} reservas vencidas liberadas", liberadas);
    }
    return liberadas;
  }
}
