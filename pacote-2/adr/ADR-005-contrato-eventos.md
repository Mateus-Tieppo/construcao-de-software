# ADR-005: Contrato de eventos, Outbox e consumo idempotente

**Estado:** Proposta para revisão do grupo · 04/11/2026

## Contexto
O enunciado exige tratar o dilema da escrita dupla e a idempotência no consumo. O Pacote 1 fixou Transactional Outbox (RNF06), deduplicação por `eventId` (RNF07) e DLQ após 3 tentativas (RNF08), e citou os nomes `inventario.reserva-expirada.v1` e `PagamentoConfirmado`.

## Decisão proposta
1. **Um tópico por agregado e contexto**, com o tipo do evento no envelope: `inventario.reservas.v1`, `checkout.pedidos.v1`, `ingressos.v1` etc. Chave da mensagem = ID do agregado. Assim todos os eventos de uma reserva ficam na mesma partição e em ordem.
2. O nome `inventario.reserva-expirada.v1` do Pacote 1 é substituído por `inventario.reservas.v1` com `tipo = ReservaExpirada`. Motivo: tópicos separados por tipo não garantem a ordem entre "criada", "confirmada" e "expirada" da mesma reserva.
3. Envelope comum com `eventId`, `tipo`, `schemaVersion`, `ocorridoEm` (UTC), `produtor`, `agregadoId`, `agregadoVersao`, `correlationId`, `causationId`, `traceparent` e `dados`.
4. Outbox por serviço, publicada por **um relay ativo por serviço** (eleito com `pg_try_advisory_lock`), em ordem de `id`, com produtor idempotente (`acks=all`, `enable.idempotence=true`).
5. Consumidores gravam `(grupo, eventId)` na tabela `mensagem_processada` **na mesma transação do efeito**; o *offset* só é confirmado após o *commit*.
6. Erros: 3 tentativas com *backoff* (1 s, 2 s, 4 s) dentro do consumidor, depois `<topico>.dlq` com cabeçalhos de diagnóstico.

## Alternativas
1. Debezium (CDC) lendo a Outbox: menos código de relay, mais um componente para operar. Fica como evolução.
2. Tópico por tipo de evento: mais legível, perde ordem por agregado.
3. Kafka transacional (*exactly-once*): só vale dentro do Kafka; não cobre PostgreSQL, PDF e e-mail.

## Consequências
- Entrega é **pelo menos uma vez**; o efeito é idempotente. O grupo não promete "exatamente uma vez" ponta a ponta.
- Mudanças de esquema seguem regras de compatibilidade (campos novos opcionais; remoção só em `v2`).
