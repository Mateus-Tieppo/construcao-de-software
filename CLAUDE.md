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
| `pacote-1/0X-*.md` | Esqueletos dos 5 entregáveis |

## Tarefa atual: PACOTE 1 — prazo 14/10/2026 23:59
Leia `pacote-1/PLANO-PACOTE-1.md` e preencha os 5 arquivos `pacote-1/01..05`. Siga a ordem do plano.
Diagramas em **Mermaid** dentro do Markdown (story map, navegação, ciclo do processo). Mockups: HTML/SVG simples em `pacote-1/mockups/` (low/medium fidelity basta).
No fim, gerar `pacote-1/Pacote1_Grupo9.pdf` consolidando tudo (capa com disciplina, grupo e data).

## Como trabalhar (o próprio processo SDD do grupo)
1. Antes de escrever um entregável, releia o trecho relevante do enunciado (início do contexto) e os critérios de pronto do plano (fim do contexto).
2. Escreva um entregável por vez; não tente fazer tudo num passo só.
3. Ao terminar cada um, faça uma verificação adversarial: compare item a item com a tabela "Pronto quando…" do plano e com a lista de requisitos do domínio acima, e liste o que falta antes de seguir.

## Critérios de verificação do Pacote 1
- [ ] Os 5 itens do enunciado estão presentes, cada um com título igual ao do enunciado.
- [ ] Todos os 5 módulos funcionais aparecem na Visão, no Story Map e nos RNFs.
- [ ] Histórias no formato "Como… quero… para…" com critérios de aceitação.
- [ ] RNFs com métrica verificável.
- [ ] Mapa de navegação cobre os 4 perfis.
- [ ] Processo com IA cita ciclo, ferramentas, agentes e specs.
- [ ] PDF final gerado e legível.
