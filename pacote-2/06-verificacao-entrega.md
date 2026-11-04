# 6. Revisão e Evidências da Entrega (Pacote 2)

> Auditoria de apoio, **não faz parte dos cinco entregáveis** e não entra no PDF. Produzida em 04/11/2026 pelo Claude Code seguindo `PLANO-PACOTE-2.md`, a partir da base `b211f09` (merge do PR #1) na branch `docs/pacote-2`.
> **Pendente:** revisão humana do grupo. Nenhuma proposta deste pacote foi aprovada pelo grupo até a geração deste arquivo.

## 6.1 Arquivos produzidos

| Arquivo | Conteúdo |
|---|---|
| `01-arquitetura-c4.md` | C1, C2 (2a síncrono, 2b dados e mensageria, 3 observabilidade), C3 dos 5 serviços + Realtime, 4 diagramas de sequência, WebSocket, segurança |
| `02-modelo-dados-conceitual.md` | Visão por dono, 4 diagramas ER, FSM com tabela de transições, estados auxiliares, 11 invariantes, complemento técnico |
| `03-mensageria-kafka.md` | Catálogo de 6 tópicos + DLQ, envelope, evolução, Outbox, consumo idempotente, PDF/e-mail, retries e DLQ, consumer groups, 9 cenários de falha |
| `04-pipeline-desenvolvimento.md` | Fluxo ponta a ponta, 9 jobs, esboço do `ci.yml`, Compose, build/publicação/implantação, estado real da iteração 2 |
| `05-processo-gcs.md` | Ferramentas, itens de configuração, branches, ciclo de mudança, template de PR, baselines, estado real da configuração |
| `adr/ADR-001` a `ADR-006` | Decisões novas, todas como proposta |
| `contratos/` | 7 exemplos JSON de eventos no envelope comum |
| `package.json`, `package-lock.json`, `build-pdf.mjs` | Gerador reproduzível do PDF (marked 12.0.2, mermaid 11.4.1, playwright-core 1.48.2 com o Chrome do sistema) |
| `Pacote2_Grupo9.pdf` | Consolidado dos documentos 01 a 05 |

## 6.2 Matriz de rastreabilidade (enunciado → onde está)

| Exigência | Fonte | Destino | Estado | Evidência |
|---|---|---|---|---|
| C4 nível 1: fronteiras, perfis, sistemas externos (gateway bancário, e-mail) | Enunciado, Arquitetura | 01, 1.2 | Atendido | Figura 1 com 4 perfis, Keycloak, simulador de gateway, SMTP |
| C4 nível 2: SPA, APIs, PostgreSQL/Redis, cluster Kafka, servidores WebSocket | Enunciado | 01, 1.3 | Atendido | Figuras 2a, 2b, 3 e tabela 1.3.1 (Realtime Service = servidor WebSocket) |
| C4 nível 3: controladores de borda, casos de uso, repositórios, workers do Outbox, consumidores | Enunciado | 01, 1.4 | Atendido | Figuras 4 a 8, uma por serviço; `OutboxRelay`, `ExpiracaoWorker` e consumidores em cada C3 |
| Modelo de Dados Conceitual | Enunciado, Pacote 2 | 02 | Atendido | Figuras 13 a 17, cardinalidades e donos |
| FSM Criado, Reservado, Aguardando, Pago, Expirado | Enunciado, Checkout | 02, 2.4 | Atendido | Figura 18 + tabela de transições; nenhum estado novo |
| Estrutura de mensageria do Kafka | Enunciado, Pacote 2 | 03 | Atendido | Tabela 3.2, Figura 19 |
| Dual-write e idempotência no consumo | Enunciado, Arquitetura | 03, 3.4 a 3.7; 01, 1.5.4 | Atendido | Figuras 12, 20, 21 |
| Idempotency-Key em cabeçalhos HTTP | Enunciado, Checkout | 01, 1.5.4 | Atendido | Tabela de regras + Figura 12 |
| Pipeline: CI a cada PR ou merge em `develop` com linters, testes e compilação | Enunciado | 04, 4.2 e 4.3 | Descrito (não implementado) | Figura 22, tabela de jobs, esboço YAML |
| ≥ 70% de cobertura de domínio; Testcontainers PostgreSQL e Kafka (e Redis) | Enunciado | 04, 4.3 | Descrito | Jobs `unit` e `integration` |
| docker-compose completo com UI de mensageria | Enunciado | 04, 4.4 | Descrito (não implementado) | Tabela de serviços do Compose |
| Processo de GCS: ferramenta, branches, regras de uso | Enunciado, Pacote 2 | 05 | Atendido | 5.1, 5.3, 5.4, 5.5, 5.7 |
| Microsserviços; Keycloak ou Cognito; Kafka | Enunciado | 01, ADR-001, ADR-004 | Atendido | 1.3.1 |
| Zero overselling; reserva de 10 min; alocação e devolução atômicas | Enunciado, Inventário | 01, 1.4.1 e 1.5.1/1.5.3; 02, 2.6; ADR-002 | Atendido | Invariantes I1, I2, I3 |
| PDF com carimbo de tempo e QR Code; atualização em tempo real | Enunciado, Ingressos | 01, 1.4.3 e 1.6; 03, 3.7 | Atendido | Figura 6 |
| Telemetria: ingestão de navegação e compras, painel | Enunciado, Telemetria | 01, 1.4.5; 03, 3.2.1 | Atendido | Figura 8 |
| RBAC 4 papéis, JWT stateless, refresh em chave-valor | Enunciado, IAM | 01, 1.4.4 e 1.7; ADR-004 | Atendido como proposta (spike pendente) | Figura 7 |

### 6.2.1 Histórias e RNFs pedidos no plano

| Item do plano (Etapa 1) | Onde |
|---|---|
| US18, US19, US21 a US25, US29 | 01 (1.4, 1.5, 1.8); 02 (2.4, 2.6); 03 (3.6, 3.7) |
| US32, US33 | 04 (4.4, 4.6) |
| RNF01 a RNF08 | 01 (1.4.1, 1.4.2, 1.5); 02 (2.6); 03 (3.4 a 3.8) |
| RNF18 a RNF29 | 01 (1.3.2, 1.4.4, 1.6, 1.7, Figura 3); 03 (3.3) |
| RNF34, RNF37 a RNF40 | 01 (1.4); 04 (4.3 a 4.5); 05 (5.7, 5.8) |

## 6.3 Questões da seção 4 do plano: como foram resolvidas

| Questão | Resolução | Onde |
|---|---|---|
| Expiração estrita versus tolerância de 5 s | Reserva inválida exatamente em `expira_em` (relógio do PostgreSQL); a tolerância do RNF03 vale só para a limpeza física e as projeções | ADR-002 itens 3 a 5; 01, Figura 11 |
| Redis/lock versus PostgreSQL | Posse decidida só por `UPDATE` condicional no PostgreSQL; Redis guarda cache, projeções, *rate limit*, sessões e revogação | ADR-002 item 6; 01, 1.3.1 |
| Pagamento versus expiração entre bancos | Inventário é o ponto de decisão; simulador em duas fases; pagamento tardio cancela a autorização e vira Expirado (`PAGAMENTO_TARDIO`) | ADR-003; 01, Figuras 10 e 11; 02, 2.4 |
| Keycloak e refresh tokens em chave-valor | IAM como BFF guarda o refresh do Keycloak cifrado no Redis; SPA recebe identificador opaco; rotação, detecção de reuso e revogação ≤ 5 s pelo Gateway. Marcado como proposta pendente de spike | ADR-004; 01, 1.4.4 |
| Repetição HTTP | Escopo usuário + rota + chave; hash do pedido; 409 em andamento, 422 corpo diferente (US22), reconciliação após 30 s; retenção 24 h | 01, 1.5.4 |
| Ordem, retries e consumidor | Chave por agregado, `agregadoVersao`, transições guardadas, Inbox na mesma transação do efeito; sem ordem global entre tópicos | 03, 3.6 e 3.8 |
| Escopo atual versus compromissos futuros | Estado real registrado: nenhum código, Compose, workflow, `develop`, tag ou proteção de branch | 04, 4.6; 05, 5.8 |

## 6.4 Revisão cruzada (inconsistências encontradas e corrigidas)

| # | Problema encontrado | Correção |
|---|---|---|
| 1 | Pacote 1 previa "lock com TTL no Redis" para reserva; isso conflita com PostgreSQL como fonte da verdade | ADR-002 substitui por TTL no banco; registrado que a spec da US18 deve ser atualizada no Pacote 3 |
| 2 | Pacote 1 usou `inventario.reserva-expirada.v1` (tópico por tipo), o que perde a ordem por reserva | ADR-005 e 03, 3.2 justificam `inventario.reservas.v1` com tipo `ReservaExpirada`; `PagamentoConfirmado` mantido |
| 3 | RNF04 lista as transições sem `Aguardando → Reservado`, mas US21 e US24 a incluem | O documento 02 mantém a transição de US21/US24 (decisão do Pacote 1). **Pendente para o grupo:** atualizar o texto do RNF04 no Pacote 3 |
| 4 | US19 diz que pagamento às 20:09:59 prevalece, sem definir o que é "pagamento confirmado" entre dois bancos | ADR-003 define: confirmação comitada no Inventário antes de `expira_em` |
| 5 | Exemplo de evento no documento 03 usava `assentoId: "A-12"` enquanto os contratos usam UUID | Exemplo corrigido para UUID + `descricao` |
| 6 | Documento 01 citava tabela `envio_email`, inexistente no modelo de dados | Trocado por `NOTIFICACAO`, como no documento 02 |
| 7 | Frases que atribuíam decisões ao grupo ("decidimos", "o grupo decidiu") em escolhas novas | Reescritas como proposta |
| 8 | Conferência de nomes: serviços, tópicos e grupos entre 01 (Figura 2b), 03 (tabelas 3.2 e 3.9) e 02 (Outbox/Inbox por serviço) | Consistentes após a revisão: produtores IAM, INV, CHK, TKT via Outbox e TEL direto; Inbox em INV, CHK, TKT, TEL |

## 6.5 Comandos realmente executados e resultados

| Comando | Resultado |
|---|---|
| `git clone` / `git pull` do repositório; `git ls-remote --tags origin` | Base `b211f09`; nenhuma tag remota; branches remotas só `main` e `docs/plano-pacote-2` |
| `npm install` em `pacote-2/` | 142 pacotes, `package-lock.json` gerado |
| `node build/check.mjs 0*.md` (parser do Mermaid em cada bloco; script auxiliar não versionado, em `build/`) | 25 diagramas, todos válidos |
| `node build-pdf.mjs` | `Pacote2_Grupo9.pdf` gerado; script falha se algum diagrama ficar sem SVG |
| `pdfinfo` | 42 páginas (35 A4 retrato, 7 A4 paisagem para diagramas largos) |
| `pdftotext` + busca | Texto pesquisável; nenhum bloco Mermaid cru, erro de renderização ou marcador pendente |
| `pdftoppm` + inspeção visual | Todas as 42 páginas olhadas; correções aplicadas: diagramas largos movidos para páginas em paisagem, legendas e títulos mantidos com o diagrama, rótulos "1." trocados (o Mermaid os lia como lista), três diagramas reorganizados para ficarem legíveis |
| `python3 -m json.tool` nos contratos | 7 arquivos JSON válidos |

**Não executados (não existe o que executar):** `mvn verify`, `npm ci` da SPA, `docker compose up`, GitHub Actions. Não há código de aplicação, Compose nem workflow no repositório.

## 6.6 Critérios finais do plano (seção 5)

| Critério | Situação |
|---|---|
| Cinco documentos completos com os títulos exigidos e fontes rastreáveis | Feito |
| C4 com C1, C2, C3, quatro perfis, cinco módulos e sistemas externos | Feito |
| Dados com cardinalidades, propriedade por serviço, FSM e invariantes | Feito |
| Zero overselling, reserva de 10 min e corrida pagamento/expiração com decisão explícita | Feito (ADR-002, ADR-003) |
| Kafka com contratos, partição/ordem, grupos, Outbox, idempotência, retries, DLQ e replay | Feito |
| Pipeline com PR + merge em `develop`, lint, testes, compilação, cobertura ≥ 70%, Testcontainers e Compose | Feito como descrição |
| GCS com branches, revisão humana, baselines e rastreabilidade | Feito |
| Exigências, decisões do Pacote 1 e propostas diferenciadas | Feito (rótulos em todos os documentos) |
| Nenhuma execução, teste, proteção, infraestrutura ou aprovação alegada sem evidência | Feito (04, 4.6; 05, 5.8; este arquivo) |
| Diagramas renderizam e PDF inspecionado página por página | Feito |
| `contexto/` e `pacote-1/` preservados; nenhum segredo | Feito: nenhum arquivo dessas pastas alterado; contratos usam dados fictícios |

## 6.7 Limitações e pendências reais

1. **Revisão do grupo:** Gabriel e Filipe precisam ler e aprovar (ou ajustar) os ADRs 001 a 006 e os rótulos de proposta antes do envio.
2. **Envio ao Moodle:** não foi feito. Na tela de envio, conferir formato e tamanho aceitos (a tarefa não informa limites) e anexar `pacote-2/Pacote2_Grupo9.pdf` (≈ 0,85 MB).
3. **Spike do ADR-004** (Keycloak + refresh no Redis) fica para o início do Pacote 3.
4. **Esqueleto, Compose e CI** (compromisso da iteração 2 no processo do Pacote 1) não foram iniciados; propostos como primeira tarefa da iteração 3.
5. **Configuração do GitHub:** criar `develop`, proteção de branches e as tags `pacote-1` (commit `644f965`) e `pacote-2` exige um integrante com permissão de admin.
6. **Atualizações no Pacote 3:** texto do RNF04 (transição `Aguardando → Reservado`), spec da US18 (lock no PostgreSQL) e US19 (definição de "pagamento confirmado").
7. Diagramas de componentes mostram os componentes principais; nomes de classes podem mudar na implementação.
8. Os arquivos fonte Markdown usam `%% paisagem` na primeira linha de alguns diagramas: é um comentário Mermaid usado só pelo gerador do PDF e não aparece no GitHub.
