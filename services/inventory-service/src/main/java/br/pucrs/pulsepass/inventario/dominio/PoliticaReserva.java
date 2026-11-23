package br.pucrs.pulsepass.inventario.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Regras puras da reserva (sem Spring nem banco).
 * Enunciado: expiração estrita em 10 minutos. Pacote 1 (US18): 1 a 6 assentos por pedido, tudo ou nada.
 */
public final class PoliticaReserva {

  public static final Duration VALIDADE = Duration.ofMinutes(10);
  public static final int MAX_ASSENTOS = 6;

  private PoliticaReserva() {
  }

  public static List<UUID> validarEOrdenar(List<UUID> assentos) {
    if (assentos == null || assentos.isEmpty()) {
      throw new RegraNegocioException("ASSENTOS_OBRIGATORIOS", "Informe ao menos um assento");
    }
    if (assentos.size() > MAX_ASSENTOS) {
      throw new RegraNegocioException("LIMITE_ASSENTOS", "No máximo " + MAX_ASSENTOS + " assentos por pedido");
    }
    if (new HashSet<>(assentos).size() != assentos.size()) {
      throw new RegraNegocioException("ASSENTO_REPETIDO", "Assento repetido no pedido");
    }
    // ordem fixa de bloqueio evita deadlock entre reservas concorrentes (ADR-002)
    return assentos.stream().sorted().toList();
  }

  public static Instant expiracao(Instant criadaEm) {
    return criadaEm.plus(VALIDADE);
  }

  /** Tudo ou nada: o número de linhas atualizadas precisa ser igual ao de assentos pedidos. */
  public static void exigirTodosBloqueados(int pedidos, int bloqueados) {
    if (bloqueados != pedidos) {
      throw new RegraNegocioException("ASSENTO_INDISPONIVEL",
          "Um ou mais assentos já foram reservados ou vendidos");
    }
  }

  public static boolean podeConfirmar(EstadoReserva estado, Instant expiraEm, Instant agora) {
    return estado == EstadoReserva.ATIVA && agora.isBefore(expiraEm);
  }

  public static boolean venceu(EstadoReserva estado, Instant expiraEm, Instant agora) {
    return estado == EstadoReserva.ATIVA && !agora.isBefore(expiraEm);
  }
}
