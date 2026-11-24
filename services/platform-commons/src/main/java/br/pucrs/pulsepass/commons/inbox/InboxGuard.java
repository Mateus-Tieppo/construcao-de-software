package br.pucrs.pulsepass.commons.inbox;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox idempotente (RNF07): registra (grupo, eventId) na mesma transação do efeito.
 * Retorna false quando o evento já foi processado por este consumer group.
 */
@Component
public class InboxGuard {

  private final JdbcTemplate jdbc;

  public InboxGuard(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public boolean primeiraVez(String grupo, UUID eventId) {
    int linhas = jdbc.update("""
        INSERT INTO mensagem_processada (grupo, event_id, processado_em)
        VALUES (?, ?, now()) ON CONFLICT DO NOTHING
        """, grupo, eventId);
    return linhas == 1;
  }
}
