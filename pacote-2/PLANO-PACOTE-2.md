# Plano de execução — Pacote 2: Arquitetura e Infraestrutura de Desenvolvimento

> PulsePass · Grupo 9 · PUCRS · Construção de Software 2026/2 · Turma 330.
> Preparado em 04/11/2026 para execução pelo Claude Code. Este arquivo é um plano, não a entrega concluída.
> **Prazo confirmado no Moodle: 04/11/2026 às 23:59, horário exibido na conta (America/Sao_Paulo).**

## 1. Fontes e escopo confirmado

- [Sala de Entrega — Pacote 2](https://moodle.pucrs.br/mod/assign/view.php?id=3923525): prazo acima; consultada em 04/11/2026. Nenhum envio havia sido feito no momento da consulta. Esse estado pode mudar.
- [Enunciado do Trabalho](https://moodle.pucrs.br/mod/resource/view.php?id=3923491): PDF `Projeto_Final_Construcao_Software-2026-2.pdf`, versão do recurso `content/2`, conferido em 04/11/2026. A página 4 confirma os cinco entregáveis abaixo.
- `contexto/01-enunciado-projeto-final.md`: transcrição existente, compatível com os itens do Pacote 2 e as restrições arquiteturais do PDF consultado. Preservar `contexto/`.
- `pacote-1/01-processo-desenvolvimento-ia.md`, `02-documento-visao.md`, `03-backlog-story-mapping.md`, `04-requisitos-nao-funcionais.md` e `05-mockups-mapa-navegacao.md`: decisões e compromissos existentes do grupo.
- Base inspecionada: commit `644f9653dd88456d1c0bebe72309fd87c2366528`. Contém documentação, mockups e PDF do Pacote 1; não contém serviços Java, SPA implementada, Compose nem workflows de CI.

**Exigência explícita desta entrega:** cinco descrições de arquitetura/infraestrutura. O produto implementado aparece nos Pacotes 3 e 4. A seção 1.1 do processo do grupo também prometeu esqueleto dos serviços, Compose e CI na iteração 2; registrar esse compromisso e seu estado real, sem apresentá-lo como uma sexta exigência explícita da lista do professor.

**Prioridade:** concluir os cinco documentos coerentes e o material de entrega antes de iniciar implementação de aplicação. Não construir o produto inteiro neste pacote. O formato PDF consolidado é uma escolha de organização do grupo, seguindo o Pacote 1; a página da tarefa consultada não informa formato obrigatório, quantidade ou tamanho máximo dos arquivos. Conferir esses limites na tela de envio antes de submeter.

## 2. Resultado esperado e arquivos

Criar os arquivos abaixo em português. Os cinco documentos devem usar os títulos do enunciado:

| Arquivo | Título/conteúdo | Pronto quando |
|---|---|---|
| `01-arquitetura-c4.md` | Descrição da arquitetura no modelo C4 | C1, C2 e C3 legíveis, cinco módulos, responsabilidades, protocolos, segurança e fluxos críticos explicados |
| `02-modelo-dados-conceitual.md` | Descrição do Modelo de Dados Conceitual | Entidades, relacionamentos, cardinalidades, propriedade por serviço e invariantes consistentes |
| `03-mensageria-kafka.md` | Descrição da estrutura de mensageria do Kafka | Catálogo de tópicos, produtores, consumidores, chaves, esquemas, ordenação, Outbox, deduplicação e recuperação |
| `04-pipeline-desenvolvimento.md` | Descrição esquemática da Pipeline de desenvolvimento | Diagrama, gatilhos, jobs, dependências, critérios de falha, artefatos, Compose e diferença entre planejado/executado |
| `05-processo-gcs.md` | Descrição do Processo de GCS | Ferramentas, branches, PR/revisão, versionamento, baselines, configuração, mudanças e rastreabilidade |
| `06-verificacao-entrega.md` | Revisão e evidências | Matriz requisito → seção/diagrama; inconsistências resolvidas; comandos realmente executados e pendências |
| `Pacote2_Grupo9.pdf` | Consolidado dos documentos 01–05 | Capa correta, sumário, figuras renderizadas, texto pesquisável, sem cortes nem placeholders |

Arquivos auxiliares recomendados: `pacote-2/adr/`, `pacote-2/diagramas/`, `pacote-2/contratos/` e `pacote-2/build-pdf.mjs` com dependências reproduzíveis. Os documentos 01–05 são a entrega central; 06 é uma auditoria de apoio. Não publicar este plano ou o prompt como se fossem os cinco entregáveis.

## 3. Ordem de trabalho

### Etapa 0 — Diagnóstico e decisões

1. Leia `CLAUDE.md`, o enunciado e este plano. Leia Visão, RNFs e o processo do Pacote 1; consulte histórias específicas ao detalhar cada módulo.
2. Confira `git status`, branch e arquivos existentes. Preserve trabalho em andamento e os arquivos históricos de `pacote-1/` e `contexto/`.
3. Crie `06-verificacao-entrega.md` com checklist inicialmente desmarcado e tabela: requisito, fonte, seção de destino, estado, evidência.
4. Liste decisões existentes versus novas propostas. Use **[Decisão do grupo — Pacote 1]** para escolhas já documentadas e **[Proposta para revisão do grupo]** para escolhas novas; não atribua aprovação inexistente.
5. Registre ADRs curtos (contexto, decisão proposta, alternativas, consequências e estado) para fronteiras dos serviços, concorrência/expiração, integração IAM/Redis, contrato de eventos e fluxo de entrega.

### Etapa 1 — Arquitetura C4 (`01`)

- **C1 — Contexto:** fronteira PulsePass; Cliente/Comprador, Organizador, Operador e Admin; provedor de identidade; gateway financeiro simulado; serviço de notificações de teste. Identifique o que pertence ao sistema e o que é dependência externa.
- **C2 — Contêineres:** SPA React/TypeScript; Spring Cloud Gateway; IAM/governança; Inventário; Checkout; Ingressos/Notificações; Telemetria; Keycloak; PostgreSQL com propriedade lógica por serviço; Redis; Kafka e Kafka UI; componente WebSocket; simulador financeiro; servidor de e-mail; Prometheus/Grafana e rastreamento. Explique processos implantáveis, dados, tecnologias, protocolos e fronteiras de confiança. Um contêiner C4 não é necessariamente um contêiner Docker.
- **C3 — Componentes:** detalhar cada backend em diagramas separados, mantendo prioridade em Inventário, Checkout e Ingressos. Mostrar controllers/adaptadores, autenticação/autorização, casos de uso, domínio/FSM, portas/repositórios, workers de expiração/Outbox, consumidores Kafka e adaptadores externos. Não substituir C3 por uma repetição do C2.
- Nomeie relações com finalidade e protocolo; coloque título, escopo e legenda em cada figura. Use Mermaid renderizável; a clareza dos níveis C4 importa mais que a sintaxe específica do renderizador.
- Acrescente diagramas de sequência para: reserva concorrente; pagamento → Outbox → Kafka → ingresso; expiração concorrendo com confirmação; repetição HTTP/evento e recuperação após falha do broker.
- Cubra o caminho WebSocket e o controle de acesso a canais por usuário/evento; deixe explícito como múltiplas instâncias entregariam mensagens aos clientes conectados.
- Rastreie US18–19, US21–25, US29, US32–33 e RNF01–08, RNF18–29, RNF34, RNF37–40.

### Etapa 2 — Dados conceituais (`02`)

- Desenhe um modelo conceitual de negócio com cardinalidades e descrições: Usuário/referência do sujeito de identidade, Perfil/Papel, Organizador, Evento, Setor, Assento, Lote, Reserva, Item de Reserva, Pedido, Item de Pedido, Pagamento, Ingresso, Validação/Check-in, Notificação, Registro de Auditoria e Evento/Agregação de Telemetria. Justifique entidades adicionais e associação entre lote e assento.
- Identifique o serviço proprietário de cada entidade. Referências entre serviços são identificadores/contratos, não autorização para joins ou FKs entre bancos independentes.
- Separe os conceitos do negócio de um **complemento técnico** para Outbox, mensagens processadas/Inbox, registros de idempotência, cache e sessões/refresh tokens. Um dump SQL não substitui o modelo conceitual.
- Descreva atributos relevantes (IDs, estados, valores monetários, instante de expiração, versão), unicidades, integridade, retenção e dados pessoais mínimos. Use UTC internamente; apresentação em pt-BR/America/Sao_Paulo.
- Explique atomicidade de vários assentos, propriedade de reserva, limite de validade, assento vendido, PDF/QR assinado e validação única de ingresso. Não use a unicidade do ingresso como única garantia contra overselling no ato da reserva.
- Inclua FSM com os cinco estados obrigatórios: Criado, Reservado, Aguardando, Pago, Expirado; tabela de transições válidas, pré-condições e efeitos. Trate falha/recusa de pagamento e cancelamento sem inventar silenciosamente novos estados.

### Etapa 3 — Mensageria Kafka (`03`)

- Produza tabela para cada tópico: nome/versionamento, eventos, produtor, consumer groups/consumidores, chave de partição, ordenação necessária, partições, retenção, replicação no ambiente local e destino de falhas. Valores novos são propostas, não exigências do professor.
- Cubra reserva criada/expirada/liberada, confirmação de pagamento, ingresso emitido e navegação/compras para telemetria; auditoria/notificação conforme o desenho. Preserve ou justifique nomes já exemplificados no Pacote 1, como `inventario.reserva-expirada.v1` e `PagamentoConfirmado`.
- Defina envelope com `eventId`, tipo, `schemaVersion`, instante UTC, produtor, agregado e correlação/causalidade/trace; inclua exemplos JSON e regras de evolução compatível. Não transportar JWT, refresh token ou dados financeiros sensíveis.
- Documente transação local que grava domínio + Outbox, worker de publicação, confirmação pelo broker e marcação de envio. Mostre a janela de duplicação após publicar e antes de marcar como enviado.
- Explique entrega pelo menos uma vez e **efeitos idempotentes**, sem prometer exatamente uma vez entre PostgreSQL, Kafka, PDF e e-mail. Deduplicação e efeito persistido devem ter uma fronteira transacional clara; offset só avança depois do processamento seguro.
- Para PDFs/notificações, descreva jobs e identificadores estáveis, recuperação de falhas parciais e limites de idempotência do provedor externo/simulador.
- Defina retries com backoff, DLQ após 3 tentativas (RNF08), alerta, inspeção e reprocessamento idempotente. Explique o impacto de retries/DLQ sobre a ordem de eventos do mesmo agregado.
- Explique isolamento dos consumer groups: Ingressos e Telemetria devem ambos receber os eventos que lhes interessam, sem competir dentro do mesmo grupo. Não dependa de ordem global entre tópicos.
- Inclua cenários: Kafka indisponível, mensagem duplicada, fora de ordem, malformada, replay, reinício do consumidor e erro após efeito parcial.

### Etapa 4 — Pipeline e infraestrutura (`04`)

- Diagrama: história/issue → spec → branch → implementação e testes locais → PR → lint/segredos → unitários/cobertura → integração/contratos/arquitetura → build → revisão humana → merge em `develop` → promoção/release em `main`.
- **Obrigatório no desenho:** CI em cada PR e em merge/push para `develop`; lint, testes e compilação. Proponha gatilhos GitHub Actions `pull_request` e `push` em `develop`, sem confundir merge com evento chamado `merge`.
- Tabela de jobs com dependências, ambiente, comandos previstos, critério de falha e artefatos. Cobertura de domínio ≥ 70%; integração com Testcontainers reais de PostgreSQL/Kafka e Redis quando usado. Preserve as metas adicionais do grupo (FSM, ArchUnit, PIT, segurança), identificando-as como tais.
- Descreva provisionamento Docker no runner, disponibilidade de Testcontainers, caches, fixação de versões e publicação dos relatórios. Testes de carga são bônus no enunciado; não bloqueiam a documentação do Pacote 2.
- Descreva Compose: backend, frontend, bancos, Redis, Kafka, Kafka UI, Keycloak, e-mail de teste e observabilidade; redes, volumes, health/readiness, inicialização de tópicos/realm/bancos, configuração e recursos necessários.
- Diferencie build de imagem, publicação no GHCR e implantação. A visão atual prevê ambiente local/acadêmico; não incluir deploy em AWS ou outro serviço pago sem nova decisão do grupo. O fórum AWS Academy não altera sozinho o escopo da entrega.
- Registre o compromisso de esqueleto + Compose + CI da iteração 2: o que já existe, o que falta e plano de implantação. Se houver tempo após os cinco documentos e o PDF, preparar scaffold como tarefa adicional claramente marcada; não gerar workflow que aponta para arquivos inexistentes nem declarar CI verde sem execução.
- Comandos `mvn verify`, `npm ci` e `docker compose ...` só podem ser reportados como executados se os respectivos projetos/arquivos existirem e houver saída verificável. Caso contrário, rotular como comandos previstos.

### Etapa 5 — GCS (`05`)

- Ferramentas: Git/GitHub, Issues, PRs e Actions; itens controlados: specs, fontes, contratos OpenAPI/eventos, migrações, Compose, workflows, documentação, dependências e PDFs de entrega.
- Preservar o fluxo definido no Pacote 1: `main` para baselines/entregas; `develop` para integração; `feature/USxx-descricao`, `fix/...`, `docs/...`; PR, CI verde e uma aprovação humana de outro integrante; squash em `develop`; tags `pacote-N`.
- Explicar abertura de issue, ligação à US/RNF/spec/ADR, commits convencionais, atualização da branch, resolução de conflitos, review, merge, release, rollback por revert e atualização de documentos.
- Incluir template de PR e checklist (escopo, contratos, testes, evidências, segredos e atualização documental); declarar autoria/uso de IA com verificação humana.
- Tratar configuração sem segredos versionados, exemplos sanitizados, migrações rastreáveis e versões de eventos. O histórico de releases deve permitir reproduzir uma entrega.
- **Estado real:** na inspeção inicial só existia a branch remota `main`; proteção de branches/CI/revisores não foram verificados como configurados. Descrever essas regras como processo proposto/definido, não como proteção técnica já ativa. Não mudar permissões/proteções nem fabricar aprovação.

### Etapa 6 — Revisão cruzada e PDF

1. Compare os cinco documentos entre si: mesmos serviços, entidades, eventos, estados, tecnologias e nomes; todo produtor/consumidor existe no C2/C3; toda entidade tem dono; todo fluxo possui caminho de falha.
2. Complete a matriz de rastreabilidade e corrija as inconsistências de risco listadas na seção 4.
3. Gere `Pacote2_Grupo9.pdf` com os cinco documentos na ordem do enunciado; capa com disciplina, professor, Grupo 9 e integrantes já identificados no `CLAUDE.md`, data 04/11/2026 e URL do repositório.
4. Use `pacote-1/build-pdf.mjs` apenas como referência: o arquivo depende de `marked` e `mermaid` locais e gera HTML, não o PDF diretamente; não há manifesto de dependências versionado na base inspecionada. Criar um processo reproduzível em `pacote-2/`, com versões/lockfile e instruções, sem modificar o gerador do Pacote 1.
5. Renderize todos os diagramas e inspecione todas as páginas do PDF (tabelas, fontes, cortes, rodapés, nomes e links). Não deixar blocos Mermaid crus, páginas vazias acidentais, texto minúsculo ou `TODO`.
6. Registre em `06` o caminho do PDF, quantidade de páginas, verificações efetivas e limitações reais. Entregue ao usuário os caminhos/links e resumo do que falta para submissão. **Não enviar automaticamente ao Moodle.**

## 4. Questões arquiteturais que exigem resolução explícita

Estas são observações de revisão do planejamento, não alterações já aprovadas do Pacote 1:

| Questão | Decisão que o Claude deve documentar e verificar |
|---|---|
| Expiração estrita versus tolerância de 5 s nos RNFs | Reserva torna-se inválida exatamente em `expiraEm` (600 s); qualquer tolerância operacional de limpeza/projeção não permite pagamento/uso após esse instante. Explicar validação autoritativa, relógio e varredura de recuperação. |
| Redis/lock versus PostgreSQL | O Pacote 1 prevê Redis e PostgreSQL como fonte de verdade. Definir a transação/controle de concorrência durável que impede dois donos de um assento; TTL, cache ou lock isolado não bastam em falha/reinício. Registrar o papel exato de Redis. |
| Pagamento versus expiração entre bancos distintos | Definir ponto de decisão no Inventário, protocolo idempotente de confirmação, vínculo reserva/pedido e tratamento de timeout/resposta perdida. Uma notificação Kafka atrasada não pode vender assento já realocado. Pagamento tardio deve ser rejeitado/compensado no simulador, sem emitir ingresso nem reverter silenciosamente Expirado para Pago. |
| Keycloak e refresh tokens em chave-valor | Não afirmar que Keycloak usa Redis por padrão. Especificar integração do componente IAM/BFF/adaptador, propriedade da sessão, armazenamento protegido, rotação e revogação, evitando duplicar um provedor de identidade. Se faltar validação técnica, marcar proposta/pendência. |
| Repetição HTTP | Chave associada ao usuário/escopo e hash do pedido; mesma chave com conteúdo diferente é conflito; disputa concorrente tem um único efeito e resposta recuperável. Definir retenção e comportamento enquanto a primeira requisição está em andamento. |
| Ordem, retries e consumidor | Usar chave/versão do agregado e transições guardadas; particionamento sozinho não resolve ordem entre tópicos ou efeitos externos. Deduplicar sem descartar evento antes de persistir seu efeito. |
| Escopo atual versus compromissos futuros | Cinco documentos prontos não significam produto implementado. Relatar o scaffold de I2 e US32/33 conforme estado real; marcar metas de RNFs como metas, não resultados medidos. |

## 5. Critérios finais de aceitação

- [ ] Existem cinco documentos completos com os títulos exigidos e fontes rastreáveis.
- [ ] C4 apresenta C1, C2 e C3, quatro perfis, cinco módulos e sistemas externos.
- [ ] Dados incluem cardinalidades, propriedade por serviço, FSM e invariantes de reserva/ingresso.
- [ ] Zero overselling, reserva de 10 min e corrida pagamento/expiração têm decisões explícitas.
- [ ] Kafka inclui contratos, partição/ordem, grupos, Outbox, idempotência, retries, DLQ e replay.
- [ ] Pipeline mostra PR + merge em `develop`, lint, testes, compilação, cobertura ≥ 70%, Testcontainers e Compose completo planejado.
- [ ] GCS apresenta fluxo utilizável de branches, revisão humana, baselines e rastreabilidade.
- [ ] Escolhas do grupo, propostas novas e exigências do professor estão diferenciadas.
- [ ] Nenhuma execução, teste, proteção de branch, infraestrutura ou aprovação é alegada sem evidência.
- [ ] Todos os diagramas renderizam e o PDF consolidado foi inspecionado página por página.
- [ ] `06-verificacao-entrega.md` contém revisão cruzada e limitações reais, sem checklist marcado por intenção.
- [ ] Arquivos de `contexto/` e do Pacote 1 foram preservados; nenhum segredo foi adicionado.

## 6. Comando para iniciar no Claude Code

Use o texto de `pacote-2/PROMPT-INICIAL.md`. Execute uma etapa por vez, verificando o respectivo critério de pronto antes de avançar; priorize a entrega documental de hoje. O trabalho só termina quando os cinco documentos, o consolidado legível e a revisão estão disponíveis, ou quando um impedimento concreto está registrado com o trabalho restante discriminado.
