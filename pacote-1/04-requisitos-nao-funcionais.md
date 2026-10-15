# 4. Requisitos Não-Funcionais

> Categorias segundo a ISO/IEC 25010. Cada RNF tem métrica/alvo verificável e forma de verificação.
> Módulos: **IAM** (Identidade e Governança) · **INV** (Inventário e Concorrência) · **CHK** (Transações e Checkout) · **TKT** (Emissão de Ingressos e Notificações) · **TEL** (Telemetria e DevOps).
> Valores numéricos de desempenho são metas definidas pelo grupo para o ambiente local de referência (docker-compose em um notebook com 8 vCPU / 16 GB) **[Decisão do grupo]**.

## 4.1 Consistência, concorrência e integridade transacional

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF01 | Confiabilidade — Integridade | **Zero overselling**: um assento só pode estar associado a uma reserva ativa ou a um pedido pago por vez. | 0 assentos com mais de um pedido Pago/Reservado em qualquer teste; restrição `UNIQUE (evento_id, assento_id)` em ingressos válidos | Teste de carga (k6/Gatling) com 1.000 usuários concorrentes disputando 100 assentos + consulta SQL de duplicidades; teste de integração concorrente (Testcontainers, 50 threads no mesmo assento → exatamente 1 sucesso) | INV, CHK |
| RNF02 | Confiabilidade — Integridade | **Alocação e devolução atômicas** de assentos (operação tudo-ou-nada, inclusive para múltiplos assentos no mesmo pedido). | 0 estados parciais (ex.: 2 de 3 assentos bloqueados) após falhas injetadas | Teste de integração com falha no meio da operação; verificação de invariantes no banco | INV |
| RNF03 | Confiabilidade — Tempo | **Expiração estrita de reserva em 10 minutos**: reserva não paga é expirada e o assento volta ao estoque. | Assento disponível novamente entre 600 s e 605 s após a reserva (tolerância ≤ 5 s); pedido vai para `Expirado`; evento `ReservaExpirada` publicado | Teste com relógio controlável (`Clock` injetado) + teste de integração com TTL do Redis e job de varredura; métrica `reservas_expiradas_atraso_segundos` (p99 ≤ 5 s) | INV, CHK |
| RNF04 | Confiabilidade — Correção | **FSM do pedido**: somente transições válidas Criado→Reservado→Aguardando→Pago e {Criado, Reservado, Aguardando}→Expirado. | 100% das transições inválidas rejeitadas; cobertura de ramos da FSM ≥ 90% | Testes unitários parametrizados (todas as combinações estado × evento) + teste de mutação (PIT) com *mutation score* ≥ 70% no pacote de domínio | CHK |
| RNF05 | Confiabilidade — Idempotência HTTP | Endpoints que criam/alteram pedidos e pagamentos exigem cabeçalho **`Idempotency-Key`**; repetições retornam a mesma resposta sem novo efeito. | 0 pedidos/cobranças duplicadas em 100 reenvios da mesma chave; chave armazenada por ≥ 24 h; requisição sem chave → HTTP 400 | Teste de integração enviando a mesma requisição N vezes (incluindo concorrentemente) | CHK |
| RNF06 | Confiabilidade — Dual-write | **Transactional Outbox**: alteração de estado e registro do evento na mesma transação ACID do PostgreSQL; publicação no Kafka por *relay*/worker. | 0 eventos perdidos; atraso Outbox→Kafka p95 ≤ 2 s em operação normal | Teste de integração (Testcontainers) derrubando o Kafka durante o checkout e religando: todos os eventos são publicados depois; nenhuma escrita direta no Kafka dentro do caso de uso (verificado por ArchUnit) | CHK, INV |
| RNF07 | Confiabilidade — Idempotência no consumo | Consumidores Kafka toleram entrega *at-least-once*: mensagens duplicadas não geram efeito duplicado. | 0 ingressos duplicados para o mesmo pedido após reprocessar o tópico inteiro | Teste reenviando o mesmo evento 10× (tabela de mensagens processadas / chave de deduplicação por `eventId`) | TKT, TEL, INV |
| RNF08 | Confiabilidade — Mensagens inválidas | Mensagens que falham repetidamente vão para **DLQ** sem bloquear a partição. | Após 3 tentativas com *backoff*, mensagem em `<topico>.dlq`; consumo das demais continua | Teste de integração com mensagem envenenada | TKT, TEL |

## 4.2 Desempenho e escalabilidade (flash crowd)

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF09 | Eficiência — Tempo de resposta | Catálogo de eventos de **alta leitura** servido por cache. | p95 ≤ 200 ms a 2.000 req/s de leitura; taxa de acerto do cache ≥ 90% | Teste de carga k6; métricas Prometheus (`http_server_requests_seconds`, *hit ratio*) | INV |
| RNF10 | Eficiência — Tempo de resposta | Reserva de assento sob pico. | p95 ≤ 500 ms, p99 ≤ 1 s com 1.000 usuários virtuais simultâneos; taxa de erro 5xx < 1% | Teste de carga de abertura de vendas (rampa de 0→1.000 VUs em 10 s) | INV, CHK |
| RNF11 | Eficiência — Capacidade | **Degradação controlada**: acima da capacidade, o gateway aplica *rate limiting* por usuário/IP em vez de derrubar serviços. | Respostas HTTP 429 com `Retry-After`; nenhum serviço reinicia por OOM ou esgotamento de pool durante o teste | Teste de carga 2× acima da meta; inspeção de *restarts* dos containers e do pool HikariCP | Todos (API Gateway) |
| RNF12 | Eficiência — Assincronismo | Etapas lentas (gerar PDF, enviar e-mail) não ficam no caminho síncrono do pagamento. | Confirmação de pagamento ao comprador p95 ≤ 1 s; ingresso emitido p95 ≤ 10 s após `PagamentoConfirmado` | Métrica de latência ponta a ponta com *trace* OpenTelemetry | CHK, TKT |
| RNF13 | Escalabilidade | Serviços sem estado em memória (exceto cache), escaláveis horizontalmente; consumidores escaláveis por partição. | Escalar INV e TKT para 2 réplicas no compose sem alteração de código; tópicos com ≥ 3 partições | `docker compose up --scale` + teste de carga repetido | INV, TKT, CHK |

## 4.3 Disponibilidade e resiliência

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF14 | Confiabilidade — Disponibilidade | Disponibilidade da jornada de compra durante janela de vendas. | ≥ 99,5% das requisições com sucesso durante o teste de carga de 30 min | Relatório k6 + painel Grafana | Todos |
| RNF15 | Confiabilidade — Tolerância a falhas | Chamadas síncronas entre serviços e ao gateway de pagamento com **timeout, retry com backoff e Circuit Breaker** (Resilience4j). | Timeout ≤ 2 s; circuito abre após 50% de falhas em 20 chamadas; falha do gateway não bloqueia threads do checkout | Teste com simulador de pagamento configurado para falhar/atrasar | CHK |
| RNF16 | Confiabilidade — Recuperabilidade | Reinício de qualquer serviço não perde pedidos nem eventos. | Após `docker restart` de CHK ou TKT durante carga: 0 pedidos órfãos; consumidores retomam do último *offset* confirmado | Teste de caos manual roteirizado | CHK, TKT |
| RNF17 | Confiabilidade — Health | Todos os serviços expõem *health checks* (liveness/readiness). | `/actuator/health` em todos; compose usa `healthcheck` e `depends_on: condition: service_healthy` | Inspeção do compose + CI sobe o ambiente e checa saúde | Todos |

## 4.4 Segurança

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF18 | Segurança — Autenticidade | Autenticação **stateless via JWT** emitido pelo **Keycloak**, assinado (RS256) e validado no gateway e em cada serviço. | Access token com validade ≤ 5 min; 100% das rotas não públicas rejeitam token ausente/expirado/adulterado (HTTP 401) | Testes de integração de segurança por rota | IAM, todos |
| RNF19 | Segurança — Sessão | **Refresh tokens armazenados em chave-valor (Redis)** com rotação e revogação. | Refresh token com validade ≤ 8 h; refresh token reutilizado após rotação é rejeitado; logout/revogação efetivo em ≤ 5 s | Teste de integração com Redis (Testcontainers) | IAM |
| RNF20 | Segurança — Autorização | **RBAC** com papéis Cliente, Organizador, Operador, Admin; princípio do menor privilégio; organizador só acessa os próprios eventos. | 100% dos endpoints com papel declarado; matriz papel × endpoint coberta por testes (acesso indevido → HTTP 403) | Testes de autorização gerados a partir da matriz; ArchUnit garante anotação de segurança em todo controller | IAM, todos |
| RNF21 | Segurança — Auditoria | **Auditoria de credenciais**: login, falha de login, refresh, logout, revogação, mudança de papel, aprovação de organizador. | 100% desses eventos registrados com usuário, IP, data/hora (UTC) e resultado; registros imutáveis (*append-only*); retenção ≥ 90 dias | Teste de integração verificando registro para cada tipo de evento | IAM |
| RNF22 | Segurança — Integridade do ingresso | QR Code contém identificador e **assinatura digital** (HMAC/assinatura assimétrica) verificável; ingresso só pode ser usado uma vez. | 0 ingressos falsificados aceitos em teste; segunda leitura do mesmo QR → "já utilizado" | Testes unitários de assinatura + teste de validação dupla | TKT |
| RNF23 | Segurança — Proteção de dados | Comunicação via HTTPS no gateway; senhas apenas no Keycloak (hash); dados pessoais mínimos (LGPD); **nenhum segredo no repositório**. | 0 segredos detectados pelo *secret scanning* (gitleaks) no CI; segredos via variáveis de ambiente/`.env` fora do Git | Etapa de CI com gitleaks; revisão de PR | Todos |
| RNF24 | Segurança — Vulnerabilidades | Dependências e imagens verificadas. | 0 vulnerabilidades críticas abertas (OWASP Dependency-Check/Trivy) no merge para `main` | Etapa de CI | Todos |

## 4.5 Observabilidade e telemetria

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF25 | Manutenibilidade — Observabilidade | Métricas técnicas (RED: taxa, erros, duração) e de negócio (vendas/min, reservas ativas, expiradas, conversão) expostas para **Prometheus** e exibidas no **Grafana**. | 100% dos serviços com `/actuator/prometheus`; painéis atualizados com atraso ≤ 15 s | Inspeção dos painéis provisionados no compose | TEL, todos |
| RNF26 | Manutenibilidade — Rastreamento | **Rastreamento distribuído** (OpenTelemetry) propagando *trace id* por HTTP e cabeçalhos Kafka. | 100% das requisições de checkout com trace ponta a ponta (gateway → CHK → Kafka → TKT) | Visualização no Jaeger/Tempo durante a demo | Todos |
| RNF27 | Manutenibilidade — Logs | Logs estruturados em JSON com `traceId`, `userId` (quando aplicável) e sem dados sensíveis (tokens, cartão). | 0 tokens/senhas em logs (teste de regex no CI sobre logs dos testes de integração) | Teste automatizado + revisão | Todos |
| RNF28 | Funcionalidade — Telemetria de negócio | Ingestão e agregação contínua de eventos de navegação e compra a partir dos tópicos Kafka. | Atraso de agregação ≤ 10 s; *consumer lag* monitorado com alerta se > 10.000 mensagens por 1 min | Painel de lag no Grafana + teste de integração do agregador | TEL |
| RNF29 | Usabilidade — Tempo real | Atualização de tela em tempo real via **WebSocket** (disponibilidade de assentos, status do pedido, ingresso emitido). | Mudança refletida na tela em ≤ 2 s (p95); reconexão automática em ≤ 5 s | Teste E2E (Playwright) com dois navegadores | TKT, INV |

## 4.6 Testabilidade, manutenibilidade e qualidade de código

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF30 | Manutenibilidade — Testabilidade | Testes unitários das regras de domínio e máquinas de estado. | **≥ 70% de cobertura de linhas no domínio** (JaCoCo), ≥ 90% de ramos na FSM do pedido; CI falha abaixo do limite | Relatório JaCoCo com *quality gate* no CI | Todos |
| RNF31 | Manutenibilidade — Testabilidade | Testes de integração com **Testcontainers reais** de PostgreSQL, Kafka e Redis (sem mocks de infraestrutura). | Todo repositório, produtor/consumidor Kafka e uso de Redis com ≥ 1 teste de integração | Execução no CI (`mvn verify`) | Todos |
| RNF32 | Manutenibilidade — Eficácia dos testes | Teste de mutação no domínio. | *Mutation score* ≥ 70% (PIT) nos pacotes de domínio de INV e CHK | Relatório PIT no CI (job semanal ou em PR para `main`) | INV, CHK |
| RNF33 | Manutenibilidade — Testes de carga (bônus) | Cenários de carga versionados. | Scripts k6 para catálogo, reserva e checkout no repositório; resultados anexados aos Pacotes 3/4 | Execução documentada | INV, CHK |
| RNF34 | Manutenibilidade — Modularidade | Arquitetura em camadas/hexagonal por serviço; domínio sem dependência de framework; *Database per Service*. | 0 violações de regras ArchUnit (domínio não importa Spring/JPA/Kafka; serviço não acessa banco de outro) | ArchUnit executado nos testes | Todos |
| RNF35 | Manutenibilidade — Analisabilidade | Padrões de código e análise estática. | Checkstyle/Spotless e ESLint/Prettier sem erros; SonarLint/Sonar sem *bugs* bloqueantes | Etapa de lint no CI | Todos, SPA |
| RNF36 | Manutenibilidade — Contratos | APIs documentadas em **OpenAPI 3** e eventos Kafka com esquema versionado. | 100% dos endpoints públicos no OpenAPI; eventos com campo `schemaVersion` e mudanças retrocompatíveis | Geração do OpenAPI no build; testes de contrato dos eventos | Todos |

## 4.7 Portabilidade, implantação e CI/CD

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF37 | Portabilidade — Instalabilidade | Ambiente completo orquestrado via **`docker-compose.yml`**: serviços back-end, front-end, PostgreSQL, Redis, Kafka, **Kafka UI**, Keycloak, Prometheus, Grafana, servidor de e-mail de teste. | `docker compose up -d` sobe tudo saudável em ≤ 5 min numa máquina limpa, sem passos manuais além de copiar `.env.example` | Teste no CI (job de *smoke*) e em máquina de outro integrante | TEL, todos |
| RNF38 | Portabilidade — Adaptabilidade | Configuração externalizada (variáveis de ambiente / Spring Cloud Config). | 0 URLs/credenciais *hard-coded*; mesmo artefato roda em dev e CI | Revisão + ArchUnit/grep no CI | Todos |
| RNF39 | Manutenibilidade — CI | **Pipeline de CI no GitHub Actions** executando lint, testes (unit + integração) e build a cada Pull Request e merge em `develop`. | 100% dos PRs para `develop` com CI verde antes do merge (regra de proteção de branch); duração do pipeline ≤ 15 min | Histórico do GitHub Actions; configuração de *branch protection* | TEL |
| RNF40 | Manutenibilidade — CD | Build e publicação de imagens Docker versionadas ao fazer merge em `main`. | Imagens com tag do commit/semver publicadas no GHCR | Histórico do pipeline | TEL |

## 4.8 Usabilidade e acessibilidade

| ID | Categoria | Requisito | Métrica / alvo | Como verificar | Módulos |
|---|---|---|---|---|---|
| RNF41 | Usabilidade — Operabilidade | Compra concluída em poucos passos. | Do evento ao pagamento em ≤ 4 telas / ≤ 6 cliques | Revisão dos mockups e teste E2E | SPA |
| RNF42 | Usabilidade — Feedback | Contagem regressiva visível da reserva e mensagens claras para "assento indisponível", "reserva expirada" e "pagamento recusado". | 100% desses estados com mensagem específica na interface | Teste E2E | SPA |
| RNF43 | Usabilidade — Acessibilidade | SPA responsiva e acessível. | WCAG 2.1 nível AA nas telas do comprador (Lighthouse Accessibility ≥ 90); funciona em telas ≥ 360 px | Lighthouse no CI do front | SPA |
| RNF44 | Usabilidade — Idioma | Interface em português (pt-BR), datas no fuso America/Sao_Paulo e valores em R$. | 100% das telas | Revisão | SPA |

## 4.9 Rastreabilidade RNF × módulos

| Módulo | RNFs principais |
|---|---|
| IAM | RNF18, RNF19, RNF20, RNF21, RNF23 |
| Inventário e Concorrência | RNF01, RNF02, RNF03, RNF09, RNF10, RNF13, RNF29 |
| Transações e Checkout | RNF01, RNF04, RNF05, RNF06, RNF12, RNF15, RNF16 |
| Emissão de Ingressos e Notificações | RNF07, RNF08, RNF12, RNF22, RNF29 |
| Telemetria e DevOps | RNF25, RNF26, RNF28, RNF37, RNF39, RNF40 |
| Transversais | RNF11, RNF14, RNF17, RNF24, RNF27, RNF30–RNF36, RNF38, RNF41–RNF44 |
