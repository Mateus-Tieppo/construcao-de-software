# Trabalho Final — Construção de Software (PUCRS 2026/2) · Grupo 9

**PulsePass**: plataforma distribuída de bilhetagem, reserva e acesso para megaeventos.

[![ci](https://github.com/Mateus-Tieppo/construcao-de-software/actions/workflows/ci.yml/badge.svg)](https://github.com/Mateus-Tieppo/construcao-de-software/actions/workflows/ci.yml)

## Executar (checkpoint do Pacote 3)
Pré-requisitos: Docker com Compose v2, JDK 21 e Maven 3.9.
```bash
cp .env.example .env              # troque as senhas de exemplo
mvn -DskipTests package           # compila os serviços (services/*/target/app.jar)
docker compose up -d --build --wait
```
- SPA: http://localhost:8080 · Keycloak: http://localhost:8180 (usuários `cliente`, `organizador`, `operador`, `admin`; senha = `PULSEPASS_DEMO_PASSWORD` do `.env`)
- Kafka UI: http://localhost:8089 · Prometheus: http://localhost:9090 · Grafana: http://localhost:3000
- Fluxo de compra ponta a ponta: `export PULSEPASS_DEMO_PASSWORD=... && ./scripts/smoke.sh`
- Testes: `mvn test` (unitários + cobertura de domínio ≥ 70%) · `mvn verify` (integração com Testcontainers; requer Docker) · `cd frontend && npm ci && npm test`

| Pasta | Conteúdo |
|---|---|
| `services/platform-commons` | Envelope de eventos, Transactional Outbox, Inbox idempotente, DLQ, tópicos |
| `services/inventory-service` | Catálogo, eventos, reserva atômica de assentos, expiração em 10 min |
| `services/checkout-service` | Pedido com FSM, Idempotency-Key, pagamento em duas fases |
| `services/ticket-service` | Emissão de ingressos (PDF + QR assinado) e validação de entrada |
| `services/payment-simulator` | Simulador do gateway financeiro |
| `services/api-gateway` | Spring Cloud Gateway: JWT, RBAC, rate limit (Redis) |
| `frontend/` | SPA React + TypeScript (Vite) |
| `infra/` | Realm do Keycloak, init do PostgreSQL, tópicos Kafka, Prometheus, Grafana |
| `pacote-3/` | Checkpoint: estado do desenvolvimento, documentação atualizada e PDF |

## Como usar no VS Code + Claude Code
1. Abra esta pasta no VS Code (`File > Open Folder…`).
2. Abra o Claude Code (painel da extensão ou `claude` no terminal integrado).
3. O `CLAUDE.md` é lido automaticamente. Para executar o **Pacote 2**, use o [prompt inicial](pacote-2/PROMPT-INICIAL.md) ou peça:
   "Execute o Pacote 2 seguindo CLAUDE.md e pacote-2/PLANO-PACOTE-2.md, produzindo os cinco documentos, a revisão e o PDF consolidado."

O [plano do Pacote 2](pacote-2/PLANO-PACOTE-2.md) foi preparado a partir do enunciado e da sala de entrega consultados em 04/11/2026. **Prazo: 04/11/2026 às 23:59.** O plano ainda precisa ser executado pelo Claude Code; não representa os entregáveis concluídos.

## Entregas
| Pacote | Prazo | Pasta |
|---|---|---|
| 1 – Processo de Desenvolvimento e Requisitos | 14/10/2026 23:59 | `pacote-1/` |
| 2 – Arquitetura e Infraestrutura | 04/11/2026 23:59 | `pacote-2/` (PDF: `pacote-2/Pacote2_Grupo9.pdf`) |
| 3 – Checkpoint do Desenvolvimento | 23/11/2026 23:59 | `pacote-3/` (PDF: `pacote-3/Pacote3_Grupo9.pdf`) + código na raiz |
| 4 – Produto Completo | 02/12/2026 | — |

Sala de entrega do Pacote 1: https://moodle.pucrs.br/mod/assign/view.php?id=3923522
Sala de entrega do Pacote 2: https://moodle.pucrs.br/mod/assign/view.php?id=3923525
Sala de entrega do Pacote 3: https://moodle.pucrs.br/mod/assign/view.php?id=3936265
PDF original do enunciado: Moodle → TDE → "Enunciado do Trabalho" (texto completo em `contexto/01-enunciado-projeto-final.md`).

Para regenerar o PDF do Pacote 2: `cd pacote-2 && npm ci && npm run pdf` (requer Google Chrome instalado).

Para regenerar o PDF do Pacote 3: `cd pacote-3 && npm ci && npm run pdf` (requer Google Chrome instalado).
