# 3. Verificação da Entrega do Pacote 3

> 23/11/2026 · Auditoria adversarial feita antes do envio, como pede o processo do grupo (Pacote 1, seção 1.2). Compara a entrega com o enunciado, com os requisitos de domínio do `CLAUDE.md` e com o que foi prometido nos Pacotes 1 e 2.

## 3.1 Exigências do enunciado para o Pacote 3

| Exigência | Atendida? | Evidência |
|---|---|---|
| Link para o repositório com o estágio corrente de desenvolvimento | Sim | https://github.com/Mateus-Tieppo/construcao-de-software, PR #3 (`feature/pacote-3-checkpoint` → `develop`) |
| Documentação dos Pacotes 1 e 2 atualizada | Sim | `pacote-3/documentacao/` (10 documentos + ADR-007), mudanças listadas em `02-registro-atualizacoes.md` |

## 3.2 Requisitos de domínio (CLAUDE.md) no código

| Requisito | Situação | Onde |
|---|---|---|
| Zero overselling | Implementado e testado em CI | `ReservaService.reservar` (`UPDATE` condicional), `CHECK assento_reserva_coerente`, `ReservaConcorrenteIT` |
| Reserva expira em 10 min | Implementado e testado em CI | `PoliticaReserva.VALIDADE`, `ExpiracaoWorker`, `ReservaConcorrenteIT` |
| FSM do pedido (Criado, Reservado, Aguardando, Pago, Expirado) | Implementado e testado em CI | `EstadoPedido`, `Pedido`, `PedidoFsmTest` |
| Idempotency-Key | Implementado e testado em CI e no *smoke* | `FiltroIdempotencia`, `RegraIdempotencia`, `CheckoutIT`, `scripts/smoke.sh` |
| Transactional Outbox → Kafka | Implementado e testado em CI | `OutboxWriter`, `OutboxRelay`, `ReservaConcorrenteIT` (lê o evento do Kafka) |
| Consumo idempotente | Implementado e testado em CI | `InboxGuard`, `EmissaoIngressoIT` (entrega duplicada) |
| RBAC (Cliente, Organizador, Operador, Admin) | Implementado; 403 verificado no *smoke* | Realm do Keycloak, `ConfiguracaoGateway`, `ConfiguracaoSeguranca` de cada serviço |
| Keycloak | Implementado | `infra/keycloak/pulsepass-realm.json`, `docker-compose.yml` |
| Microsserviços | Implementado em parte | 5 serviços próprios + Keycloak; IAM, Realtime e Telemetry pendentes |
| Ingresso em PDF com QR Code | Implementado e testado em CI | `PdfJobWorker`, `GeradorPdf`, `EmissaoIngressoIT` |
| Tempo real (WebSocket) | **Pendente** | Pacote 4; a SPA consulta o pedido a cada 5 s |
| Telemetria | **Parcial** | Prometheus + Grafana; Telemetry Service pendente |

## 3.3 Itens técnicos do enunciado (Aspectos de Arquitetura e Pirâmide de Testes)

| Item | Situação |
|---|---|
| Testes unitários cobrindo máquinas de estado, ≥ 70% de cobertura de domínio | JaCoCo com `check` no pacote `dominio` de Inventário, Checkout e Ingressos; o build falha abaixo de 70% |
| Testes de integração com Testcontainers reais de PostgreSQL e Kafka | 3 classes `*IT` (12 testes) com PostgreSQL 16 e Kafka |
| Redis nos testes de integração ("se for o caso") | Não: o Redis só é usado pelo Gateway para *rate limit*; sem teste de integração ainda |
| `docker-compose.yml` com backend, frontend, bancos, Kafka e UI de mensageria | Sim, verificado pelo job de *smoke* |
| CI no GitHub Actions com linters, testes e compilação a cada PR ou merge na `develop` | Sim (`ci.yml`) |
| Testes de carga (bônus) | Não |

## 3.4 Afirmações conferidas

| Afirmação nos documentos | Conferida contra |
|---|---|
| Totais do MVP: 78 feitos, 19 parciais, 6 não iniciados (103 pontos, 22 histórias) | Pontos de cada história somados a partir de `pacote-1/03-backlog-story-mapping.md` |
| 46 testes unitários e 12 de integração passando; cobertura de domínio 96,9% / 100% / 91,7% | Logs dos jobs 2 e 3 das execuções 45284290380 e 45285399971 |
| *Smoke* ponta a ponta verde no Compose | Log do job 4 da execução 45284290380 (`SMOKE OK`) |
| Falha inicial do Testcontainers com Docker 29 e correção para 1.21.4 | Primeira execução da CI no PR #3 e commit `fix(ci)` |
| `develop` criada em 23/11/2026 | `git ls-remote` do repositório |
| Proteção de branch "não verificada" | Nenhuma configuração foi alterada nesta entrega |

## 3.5 O que fica para o grupo antes de enviar

1. Revisar o PR #3 (um integrante diferente de quem abriu) e fazer o merge na `develop`; depois, PR `develop` → `main` e *tag* `pacote-3`.
2. Revisar e aceitar (ou ajustar) o ADR-007.
3. Enviar na sala do Moodle o link do repositório e o PDF `pacote-3/Pacote3_Grupo9.pdf`.
