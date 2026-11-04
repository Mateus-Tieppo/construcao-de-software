# ADR-002: Concorrência de assentos e expiração da reserva no PostgreSQL

**Estado:** Proposta para revisão do grupo · 04/11/2026

## Contexto
O enunciado exige zero overselling e expiração estrita em 10 minutos. O Pacote 1 previa "lock com TTL no Redis" (Visão 2.7 e spec exemplo da US18) e também "PostgreSQL como fonte da verdade". O RNF03 aceita até 5 s para o assento voltar ao estoque. Um lock apenas no Redis pode ser perdido em reinício, *failover* ou pausa de GC, e o TTL do Redis não garante que o banco libere o assento junto.

## Decisão proposta
1. **A decisão de posse do assento é tomada só no PostgreSQL do Inventário**, em uma transação que faz `UPDATE assento SET estado='RESERVADO', reserva_id=:r, versao=versao+1 WHERE evento_id=:e AND id = ANY(:ids) AND estado='DISPONIVEL'` e exige que o número de linhas afetadas seja igual ao número de assentos pedidos; caso contrário, *rollback* (tudo ou nada).
2. Assentos são travados em ordem crescente de `id` para evitar *deadlock* entre reservas sobrepostas.
3. A reserva grava `expira_em = now() + interval '600 seconds'` usando **o relógio do PostgreSQL** como relógio único.
4. **Validade é autoritativa e exata:** toda operação que depende da reserva (confirmar, pagar, cancelar) testa `now() < expira_em` dentro da transação. A partir de `expira_em` a reserva é inválida, mesmo que ainda não tenha sido varrida.
5. Um *worker* de varredura roda a cada 1 s (`FOR UPDATE SKIP LOCKED`, lotes de 500), marca reservas vencidas como `EXPIRADA`, devolve os assentos e grava `ReservaExpirada` na Outbox. A tolerância de 5 s do RNF03 vale **só para essa limpeza física e para as projeções**; nunca permite pagamento após `expira_em`.
6. **Redis não decide posse.** Ele guarda: cache do catálogo, projeção do mapa de assentos para leitura, contadores de *rate limit* e sessões de refresh token (ADR-004).

## Alternativas
1. Lock distribuído no Redis (Redlock) + gravação no PostgreSQL: duas fontes de verdade, falha entre elas gera assento órfão ou duplicado.
2. `SELECT ... FOR UPDATE` seguido de `UPDATE`: correto, mas mais idas ao banco que o `UPDATE` condicional.
3. Fila única por evento (serializar todas as reservas): elimina disputa, mas vira gargalo no pico.

## Consequências
- Substitui o "lock com TTL no Redis" do Pacote 1 por TTL no banco. A spec da US18 deve ser atualizada no Pacote 3.
- Sob disputa extrema por um mesmo assento, perdedores recebem 409 rapidamente (o `UPDATE` condicional não espera o vencedor terminar além do bloqueio da linha).
- O teste de 50 threads da US18 passa a verificar o banco, não o Redis.
