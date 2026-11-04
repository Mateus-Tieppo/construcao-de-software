# CLAUDE.md — Trabalho Final de Construção de Software (PUCRS 2026/2)

## REGRAS INEGOCIÁVEIS (ler primeiro)
- Tudo em **português (pt-BR)**.
- O enunciado oficial está em `contexto/01-enunciado-projeto-final.md`. **Ele é a fonte de verdade**: não invente requisitos que contradigam o enunciado; quando extrapolar, marque como decisão do grupo.
- Requisitos do domínio que nunca podem faltar: zero overselling · reserva expira em 10 min · FSM do pedido (Criado, Reservado, Aguardando, Pago, Expirado) · Idempotency-Key · Transactional Outbox → Kafka · RBAC (Cliente, Organizador, Operador, Admin) · Keycloak/Cognito · microsserviços · ingresso em PDF com QR Code · tempo real (WebSocket) · telemetria.
- Não coloque segredos, senhas ou tokens em nenhum arquivo.
- Não altere nada em `contexto/` (é a cópia do Moodle). Trabalhe em `pacote-N/`.

## Contexto
- Disciplina: 46504-04 Construção de Software, Turma 330, Prof. Michael da Costa Mora.
- Grupo 9: Gabriel Gerhardt de Marque, Mateus Granja de Andrade Madalosso Tieppo, Filipe Tesche Roman.
- Produto: plataforma distribuída de bilhetagem, reserva e acesso para megaeventos — nome adotado: **PulsePass** (decisão do grupo, 14/10).
- Stack que a turma já usa: Java + Spring Boot + Spring Cloud, Docker Compose, Prometheus/Grafana, JUnit, testes de mutação. Front-end: React + TypeScript (Vite) — decisão do grupo. IA: Claude Code (principal), Antigravity opcional.

## Mapa da pasta
| Caminho | Conteúdo |
|---|---|
| `contexto/01-enunciado-projeto-final.md` | Enunciado completo do TF |
| `contexto/02-disciplina-moodle.md` | Cronograma, avaliação, grupo, aulas |
| `contexto/03-sdd-ia-aula09.md` | Aula de SDD/IA — base do item "processo com IA" |
| `contexto/04-exercicios-anteriores.md` | Exercícios anteriores (stack e padrões) |
| `pacote-1/PLANO-PACOTE-1.md` | **Checklist e critérios de pronto do Pacote 1** |
| `pacote-1/0X-*.md` | Documentos do Pacote 1 já produzidos; preservar como referência |
| `pacote-2/PLANO-PACOTE-2.md` | Plano, fontes verificadas, ordem de execução e critérios de pronto do Pacote 2 |
| `pacote-2/PROMPT-INICIAL.md` | Prompt para executar o Pacote 2 no Claude Code |

## Tarefa atual: PACOTE 2 — prazo 04/11/2026 23:59
Prazo conferido no Moodle em 04/11/2026: https://moodle.pucrs.br/mod/assign/view.php?id=3923525.
Leia `pacote-2/PLANO-PACOTE-2.md` e execute as etapas para produzir os cinco documentos de arquitetura/infraestrutura, a revisão e `pacote-2/Pacote2_Grupo9.pdf`.
O Pacote 1 já possui documentos, mockups e PDF; preserve `pacote-1/` e use suas decisões como referência. A tarefa atual é a entrega documental do Pacote 2; não confundir com implementação completa dos Pacotes 3/4. O plano registra separadamente o compromisso de scaffold/Compose/CI da iteração 2.
Diagramas em Mermaid renderizados no PDF. Diferencie exigência oficial, decisão já registrada e proposta para revisão. Não alegue execução de testes, CI, infraestrutura ou aprovação sem evidência. Não submeta ao Moodle automaticamente.

## Como trabalhar (o próprio processo SDD do grupo)
1. Antes de escrever um entregável, releia o trecho relevante do enunciado (início do contexto) e os critérios de pronto do plano (fim do contexto).
2. Escreva um entregável por vez; não tente fazer tudo num passo só.
3. Ao terminar cada um, faça uma verificação adversarial: compare item a item com a tabela "Pronto quando…" do plano e com a lista de requisitos do domínio acima, e liste o que falta antes de seguir.

## Critérios de verificação do Pacote 2
- [ ] C4 C1/C2/C3, dados conceituais, Kafka, pipeline e GCS completos.
- [ ] Cinco módulos e quatro perfis contemplados, com rastreabilidade para histórias/RNFs.
- [ ] Concorrência, expiração, pagamento, Outbox e idempotência resolvidos de forma consistente.
- [ ] Pipeline descreve PR/merge em develop, lint, testes, build, cobertura de domínio ≥ 70% e Testcontainers.
- [ ] Processo GCS preserva revisão humana e distingue regras documentadas de configuração efetiva.
- [ ] Checklist detalhado do plano atendido e evidências em `pacote-2/06-verificacao-entrega.md`.
- [ ] PDF consolidado gerado com diagramas renderizados e todas as páginas verificadas.
