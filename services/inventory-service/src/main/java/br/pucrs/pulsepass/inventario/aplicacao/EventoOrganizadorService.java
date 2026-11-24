package br.pucrs.pulsepass.inventario.aplicacao;

import br.pucrs.pulsepass.commons.eventos.Uuids;
import br.pucrs.pulsepass.commons.outbox.OutboxWriter;
import br.pucrs.pulsepass.inventario.dominio.EstadoEvento;
import br.pucrs.pulsepass.inventario.dominio.MapaAssentos;
import br.pucrs.pulsepass.inventario.dominio.RegraNegocioException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Criação de evento com setores, mapa de assentos e lote (US09, US10, US11) e publicação (US12). */
@Service
public class EventoOrganizadorService {

  public static final String TOPICO_CATALOGO = "inventario.catalogo.v1";

  private final JdbcTemplate jdbc;
  private final OutboxWriter outbox;

  public EventoOrganizadorService(JdbcTemplate jdbc, OutboxWriter outbox) {
    this.jdbc = jdbc;
    this.outbox = outbox;
  }

  public record NovoSetor(String nome, int filas, int assentosPorFila, String nomeLote, long precoCentavos) {
  }

  public record NovoEvento(String nome, String descricao, String localCidade, Instant inicio, List<NovoSetor> setores) {
  }

  @Transactional
  public UUID criar(UUID organizadorId, NovoEvento novo) {
    if (novo.setores() == null || novo.setores().isEmpty()) {
      throw new RegraNegocioException("SETOR_OBRIGATORIO", "Informe ao menos um setor");
    }
    if (novo.inicio() == null || !novo.inicio().isAfter(Instant.now())) {
      throw new RegraNegocioException("DATA_INVALIDA", "O evento precisa começar no futuro");
    }
    UUID eventoId = Uuids.v7();
    jdbc.update("""
        INSERT INTO evento (id, organizador_id, nome, descricao, local_cidade, inicio, estado)
        VALUES (?, ?, ?, ?, ?, ?, 'RASCUNHO')
        """, eventoId, organizadorId, novo.nome(), novo.descricao(), novo.localCidade(), Timestamp.from(novo.inicio()));
    for (NovoSetor s : novo.setores()) {
      List<String> rotulos = MapaAssentos.rotulos(s.filas(), s.assentosPorFila());
      if (s.precoCentavos() <= 0) {
        throw new RegraNegocioException("PRECO_INVALIDO", "Preço do lote deve ser positivo");
      }
      UUID setorId = Uuids.v7();
      jdbc.update("INSERT INTO setor (id, evento_id, nome, capacidade) VALUES (?, ?, ?, ?)",
          setorId, eventoId, s.nome(), rotulos.size());
      List<Object[]> lote = new ArrayList<>();
      for (String rotulo : rotulos) {
        lote.add(new Object[] {Uuids.v7(), setorId, rotulo});
      }
      jdbc.batchUpdate("INSERT INTO assento (id, setor_id, fila_numero, estado) VALUES (?, ?, ?, 'DISPONIVEL')", lote);
      jdbc.update("""
          INSERT INTO lote (id, setor_id, nome, preco_centavos, quantidade, vende_de, vende_ate)
          VALUES (?, ?, ?, ?, ?, now(), ?)
          """, Uuids.v7(), setorId, s.nomeLote() == null ? "1º lote" : s.nomeLote(), s.precoCentavos(), rotulos.size(),
          Timestamp.from(novo.inicio()));
    }
    return eventoId;
  }

  @Transactional
  public void publicar(UUID organizadorId, UUID eventoId) {
    String estado = jdbc.query("SELECT estado FROM evento WHERE id = ? AND organizador_id = ? FOR UPDATE",
        rs -> rs.next() ? rs.getString(1) : null, eventoId, organizadorId);
    if (estado == null) {
      throw new RegraNegocioException("EVENTO_INEXISTENTE", "Evento não encontrado para este organizador");
    }
    EstadoEvento novo = EstadoEvento.valueOf(estado).publicar();
    jdbc.update("UPDATE evento SET estado = ? WHERE id = ?", novo.name(), eventoId);
    Map<String, Object> dados = new LinkedHashMap<>();
    dados.put("eventoId", eventoId);
    dados.put("organizadorId", organizadorId);
    outbox.gravar(TOPICO_CATALOGO, "EventoPublicado", eventoId, 1, eventoId, eventoId, dados);
  }
}
