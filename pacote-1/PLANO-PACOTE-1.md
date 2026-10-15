# Pacote 1 — Processo de Desenvolvimento e Requisitos

**Prazo: quarta 14/10/2026, 23:59** — Sala de Entrega: https://moodle.pucrs.br/mod/assign/view.php?id=3923522
Sem instruções de formato na sala. Formato sugerido: **um único PDF** (`Pacote1_Grupo9.pdf`) com todos os itens + link para o repositório onde os `.md` vivem (eles serão atualizados nos Pacotes 3 e 4). Uma pessoa do grupo entrega.
Grupo 9: Gabriel Gerhardt de Marque, Mateus Granja de Andrade Madalosso Tieppo, Filipe Tesche Roman.

## Os 5 itens exigidos (texto literal do enunciado → o que entregar)

| # | Item do enunciado | Arquivo | Pronto quando… |
|---|---|---|---|
| 1 | "Descrição do processo de desenvolvimento suportado por IA, como descrito no enunciado" (ciclo de desenvolvimento, ferramentas, agentes e specs utilizadas) | `01-processo-desenvolvimento-ia.md` | Descreve o ciclo (fases e loops), ferramentas concretas, papéis de agentes (coordenador/implementador/verificador), formato e local das specs, controles feedforward (CLAUDE.md, skills) e feedback (testes, linters, ArchUnit, LLM-as-judge), papel humano de supervisão, e como isso conversa com GCS/CI. Tem um diagrama do ciclo. |
| 2 | Documento de Visão do Produto | `02-documento-visao.md` | Nome do produto, problema, oportunidade, stakeholders e usuários (Cliente/comprador, Organizador, Operador, Admin), visão geral, os 5 módulos, funcionalidades principais, restrições (zero overselling, reserva 10 min, microsserviços, Keycloak, Kafka), fora de escopo, critérios de sucesso. |
| 3 | Backlog com Épicos e Histórias de Usuário no formato de Story Mapping | `03-backlog-story-mapping.md` | Backbone de atividades do usuário (jornada) na horizontal, épicos, histórias "Como… quero… para…" com critérios de aceitação, organizadas em fatias de release (MVP / R2 / R3). Cobre os 5 módulos. Inclui o mapa visual (tabela ou diagrama). |
| 4 | Requisitos não-funcionais | `04-requisitos-nao-funcionais.md` | RNFs **mensuráveis** (ID, categoria, descrição, métrica/alvo, como verificar): concorrência/zero overselling, expiração 10 min, idempotência, outbox/dual-write, desempenho em flash crowd, disponibilidade/resiliência, segurança (JWT, RBAC, refresh em chave-valor, auditoria), observabilidade, testabilidade (≥70% domínio, Testcontainers), manutenibilidade, portabilidade (docker-compose), CI. |
| 5 | Mockups (low, medium ou high res) e mapa de navegação do front-end | `05-mockups-mapa-navegacao.md` + `mockups/` | Telas por perfil (comprador, organizador, operador, admin) e mapa de navegação (diagrama de telas e transições). |

## Requisitos do domínio que TÊM de aparecer nos artefatos
- Picos de tráfego na abertura de vendas (*flash crowd*); **zero overselling**; **reserva temporária expira em 10 min**; emissão segura de ingressos.
- IAM: JWT stateless + refresh token em chave-valor (Redis); RBAC Cliente/Organizador/Operador/Admin; auditoria de credenciais; cadastro de organizadores. Provedor: **Keycloak ou Cognito**.
- Inventário: lotes, mapa de assentos, catálogo de alta leitura, alocação e devolução **atômicas**, eventos de expiração no broker.
- Checkout: FSM do pedido **Criado → Reservado → Aguardando → Pago / Expirado**; **Idempotency-Key** no header; **Transactional Outbox** (ACID relacional) → Kafka; simulador de gateway de pagamento.
- Ingressos: consumo assíncrono de pagamentos confirmados; PDF com carimbo de tempo e **QR Code**; notificação multicanal; atualização em tempo real (WebSocket).
- Telemetria/DevOps: ingestão de eventos de navegação/compra, painel de métricas em tempo real, CI/CD, docker-compose.
- Arquitetura: microsserviços, **Kafka**, tratar **dual-write** e **idempotência no consumo**.

## Ordem de trabalho sugerida (com ~5h)
1. Decidir nome do produto + stack + ferramentas de IA do grupo (10 min) → preencher topo do `CLAUDE.md` e do item 1.
2. Item 2 (Visão) — base para o resto.
3. Item 4 (RNFs) — sai quase direto do enunciado.
4. Item 3 (Story Map) — maior volume.
5. Item 5 (Mockups + mapa de navegação) — gerar HTML/SVG simples ou wireframes em Mermaid/Excalidraw.
6. Item 1 (Processo com IA) — escrever descrevendo o que o grupo de fato fez (inclusive *este* uso de Claude Code com CLAUDE.md + specs).
7. Revisão contra a tabela acima → gerar PDF → enviar no Moodle.
