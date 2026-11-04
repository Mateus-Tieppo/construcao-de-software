# ADR-003: Ponto de decisão entre pagamento e expiração

**Estado:** Aceita pelo grupo em 04/11/2026

## Contexto
Checkout e Inventário têm bancos distintos. Um pagamento aprovado perto do 10º minuto pode chegar depois que o Inventário expirou a reserva e realocou o assento. Uma notificação Kafka atrasada não pode vender um assento já devolvido. A US19 diz que pagamento confirmado às 20:09:59 prevalece; a US21 diz que pagamento tardio sobre pedido Expirado gera estorno simulado.

## Decisão
1. **O ponto de decisão é o Inventário.** A venda só é efetiva quando `POST /internal/reservas/{reservaId}/confirmacao` comita no banco do Inventário com `estado='ATIVA' AND now() < expira_em`. Essa mesma transação marca os assentos `VENDIDO` e grava `ReservaConfirmada` na Outbox.
2. O simulador de gateway trabalha em **duas fases**: `autorizar` (reserva o valor) e depois `capturar` ou `cancelar`. Ordem no Checkout: autorizar → confirmar no Inventário → capturar → pedido `Pago`.
3. Se o Inventário responder `409 RESERVA_EXPIRADA`, o Checkout cancela a autorização, leva o pedido a `Expirado` (motivo `PAGAMENTO_TARDIO`) e grava `EstornoSolicitado`. Não existe transição `Expirado → Pago`.
4. Todas as chamadas carregam identificadores estáveis: `Idempotency-Key = pagamentoId` no simulador e `pedidoId` na confirmação do Inventário. Repetir a confirmação de uma reserva já confirmada para o mesmo pedido devolve o mesmo resultado.
5. Timeout ou resposta perdida: o Checkout repete com a mesma chave (no máximo 3 vezes, *backoff* exponencial, *timeout* de 2 s por chamada, RNF15). Se não houver resposta, o pedido fica `Aguardando` e um *worker* de reconciliação consulta o estado no simulador e no Inventário a cada 30 s até resolver.
6. A interpretação da US19 passa a ser: "pagamento confirmado" significa **confirmação comitada no Inventário antes de `expira_em`**. A aprovação do gateway sozinha não garante o assento.

## Alternativas
1. Saga coreografada só por eventos: a decisão ficaria espalhada e dependente de latência do broker.
2. Captura imediata e estorno quando o Inventário recusar: funciona, mas o comprador é cobrado antes da garantia do assento.
3. Estender a reserva quando o pagamento começa: contraria a expiração estrita do enunciado.

## Consequências
- Chamada síncrona Checkout → Inventário no caminho do pagamento (protegida por *timeout* e Circuit Breaker).
- O simulador precisa suportar autorização/captura/cancelamento idempotentes; é código do grupo, não um serviço real.
- A US19 e a US24 precisam ter esse texto alinhado no Pacote 3.
