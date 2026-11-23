package br.pucrs.pulsepass.ingressos.aplicacao;

import br.pucrs.pulsepass.commons.eventos.Uuids;
import br.pucrs.pulsepass.commons.inbox.InboxGuard;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RegistrarEmissao (US25): em UMA transação grava a Inbox, um ingresso PENDENTE por item e um job de PDF.
 * UNIQUE (item_pedido_id) garante um ingresso por assento mesmo com reprocessamento (RNF07, invariante I8).
 */
@Service
public class EmissaoService {

  public static final String GRUPO = "tkt-emissao";

  private final JdbcTemplate jdbc;
  private final InboxGuard inbox;

  public EmissaoService(JdbcTemplate jdbc, InboxGuard inbox) {
    this.jdbc = jdbc;
    this.inbox = inbox;
  }

  /** @return quantidade de ingressos criados (0 se o evento já tinha sido processado) */
  @Transactional
  public int registrar(JsonNode evento) {
    UUID eventId = UUID.fromString(evento.path("eventId").asText());
    if (!inbox.primeiraVez(GRUPO, eventId)) {
      return 0;
    }
    JsonNode dados = evento.path("dados");
    UUID pedidoId = UUID.fromString(dados.path("pedidoId").asText());
    UUID eventoId = UUID.fromString(dados.path("eventoId").asText());
    UUID titularId = UUID.fromString(dados.path("compradorId").asText());
    String titularNome = dados.path("compradorNome").asText("Titular");
    int criados = 0;
    for (JsonNode item : dados.path("itens")) {
      UUID ingressoId = Uuids.v7();
      int linhas = jdbc.update("""
          INSERT INTO ingresso (id, item_pedido_id, pedido_id, evento_id, titular_id, titular_nome, setor_assento, estado)
          VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDENTE') ON CONFLICT (item_pedido_id) DO NOTHING
          """, ingressoId, UUID.fromString(item.path("itemPedidoId").asText()), pedidoId, eventoId, titularId,
          titularNome, item.path("descricao").asText());
      if (linhas == 1) {
        jdbc.update("INSERT INTO job_pdf (ingresso_id, estado, tentativas) VALUES (?, 'PENDENTE', 0)", ingressoId);
        criados++;
      }
    }
    return criados;
  }
}
