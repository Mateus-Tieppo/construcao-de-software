# 2. Registro de Atualizações da Documentação

> Versão 1.0 · 23/11/2026 · Grupo 9.
> O enunciado pede, no Pacote 3, a documentação dos Pacotes 1 e 2 **atualizada** **[Enunciado]**. As versões atualizadas estão em `pacote-3/documentacao/`; as originais continuam em `pacote-1/` e `pacote-2/` como registro do que foi entregue em cada data. Todo trecho novo está marcado com **[Atualização Pacote 3]**.

## 2.1 Critério usado

A documentação foi atualizada onde o código mostrou algo diferente do planejado ou onde um texto dizia "será feito no Pacote 3". Requisitos, escopo e decisões aceitas pelo grupo **não** foram alterados sem registro: o que mudou na implementação aparece como acréscimo e as decisões novas estão no ADR-007.

## 2.2 Pacote 1

| Documento | O que mudou | Seção |
|---|---|---|
| 1. Processo de desenvolvimento com IA | Nova evidência de como o Pacote 3 foi produzido: spec, *inner loop* com Claude Code, CI como sensor determinístico (incluindo a falha do Testcontainers com Docker 29 e a correção), revisão humana pelo PR; lista do que do *harness* já está ativo e do que continua planejado | 1.10 (nova) |
| 2. Documento de Visão | Versão 1.1; situação do produto no checkpoint por perfil de usuário. Escopo e critérios de sucesso sem mudança | 2.12 (nova) |
| 3. Backlog / Story Mapping | Situação de cada uma das 22 histórias do MVP com a evidência; totais 55 feitos, 42 parciais, 6 não iniciados (de 103 pontos), classificados critério a critério | 3.6 (nova) |
| 4. Requisitos não-funcionais | Situação de verificação de cada RNF: verificado em CI, implementado sem teste dedicado ou pendente | 4.10 (nova) |
| 5. Mockups e mapa de navegação | Rotas da SPA implementada, tela correspondente de cada mockup e diferenças | 5.7 (nova) |

## 2.3 Pacote 2

| Documento | O que mudou | Seção |
|---|---|---|
| 1. Arquitetura C4 | Versão 1.1; aviso de "nenhum serviço implementado" substituído; OpenAPI publicado pelos serviços; tabela de estado por contêiner com os componentes do C3 já em código e as diferenças | Cabeçalho, 1.3.3, 1.8 (nova); rastreabilidade passou a 1.9 |
| 2. Modelo de dados conceitual | Modelo físico implementado (migrações Flyway por serviço), invariantes que viraram restrições no banco e atributos acrescentados | 2.9 (nova) |
| 3. Mensageria Kafka | Implementação real: criação de tópicos, Outbox, Inbox, DLQ, consumer groups ativos, eventos publicados e o que falta; imagem de Kafka nos testes | 3.3 (parágrafo), 3.12 (nova) |
| 4. Pipeline de desenvolvimento | Estado real substitui o aviso de "só documentação"; comparação job a job entre o desenho e o `ci.yml` real; compromisso da iteração 2 cumprido | Cabeçalho, 4.3.2 (título), 4.7 (nova); rastreabilidade passou a 4.8 |
| 5. Processo de GCS | Estado real em 23/11/2026: `develop` criada, PR #3, CI ativa, proteção de branch e tags ainda pendentes | 5.9 (nova); rastreabilidade passou a 5.10 |
| ADRs | ADR-007 (decisões de implementação: JdbcTemplate, `platform-commons`, criação de tópicos, imagem única) como proposta para revisão do grupo | `adr/ADR-007-implementacao-checkpoint.md` |

## 2.4 O que não mudou

- O enunciado e a cópia do Moodle em `contexto/` (não podem ser alterados).
- A FSM do pedido, os tópicos, os consumer groups e as invariantes do Pacote 2: o código segue o desenho.
- As metas numéricas dos RNFs: só foi acrescentada a situação de verificação.
- Os mockups em imagem: a SPA foi construída a partir deles e as diferenças estão na seção 5.7.
