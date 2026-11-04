# 1. Descrição da Arquitetura no Modelo C4

> Versão 1.0 · 04/11/2026 · Grupo 9 · Será atualizado nos Pacotes 3 e 4.
> Rótulos: **[Enunciado]** exigência do professor · **[Decisão do grupo, Pacote 1]** escolha já registrada · **[Proposta para revisão do grupo]** escolha nova deste pacote. As decisões novas estão detalhadas nos ADRs de `pacote-2/adr/`.
> Este documento descreve a arquitetura **planejada**. Nenhum serviço foi implementado até esta entrega.

## 1.1 Objetivo e leitura dos diagramas

O modelo C4 descreve o PulsePass em três níveis de zoom **[Enunciado]**:

| Nível | Pergunta que responde | Seção |
|---|---|---|
| C1 · Contexto | Quem usa o sistema e com quais sistemas externos ele conversa? | 1.2 |
| C2 · Contêineres | Quais processos implantáveis e bancos existem e como se comunicam? | 1.3 |
| C3 · Componentes | Como cada serviço de backend é organizado por dentro? | 1.4 |

Os diagramas são escritos em Mermaid (diagramas como código, versionados com o resto do projeto). Usamos sempre a mesma legenda:

| Forma / cor | Significa |
|---|---|
| Azul escuro | Pessoa (perfil RBAC) |
| Azul | Sistema PulsePass ou contêiner desenvolvido pelo grupo |
| Cinza | Sistema ou produto externo, não desenvolvido pelo grupo |
| Cilindro | Armazenamento de dados |
| Seta com rótulo | Finalidade da relação e protocolo |

Um **contêiner C4** é qualquer coisa que roda ou guarda dados separadamente (uma aplicação, um banco, um broker). Ele não é necessariamente um contêiner Docker, embora no PulsePass quase todos virem um serviço do `docker-compose.yml`.

## 1.2 Nível 1: Contexto

```mermaid
flowchart TB
    cli["<b>Cliente / Comprador</b><br/>[Pessoa]<br/>Navega, reserva, paga e recebe ingressos"]
    org["<b>Organizador</b><br/>[Pessoa]<br/>Cadastra eventos, lotes e mapas; acompanha vendas"]
    ope["<b>Operador</b><br/>[Pessoa]<br/>Valida ingressos na portaria"]
    adm["<b>Admin</b><br/>[Pessoa]<br/>Aprova organizadores, audita e monitora"]
    pp["<b>PulsePass</b><br/>[Sistema de software]<br/>Bilhetagem, reserva e acesso para megaeventos<br/>com zero overselling e reserva de 10 min"]
    idp["<b>Provedor de identidade</b><br/>[Sistema externo: Keycloak]<br/>Contas, senhas, emissão e assinatura de JWT"]
    pay["<b>Gateway financeiro</b><br/>[Sistema externo: simulador]<br/>Autoriza, captura e cancela pagamentos"]
    mail["<b>Disparo de e-mails</b><br/>[Sistema externo: SMTP de teste]<br/>Entrega comprovantes e PDF"]
    cli -- "Compra ingressos<br/>HTTPS / WebSocket" --> pp
    org -- "Gerencia eventos e vê vendas<br/>HTTPS" --> pp
    ope -- "Lê QR Code e valida<br/>HTTPS" --> pp
    adm -- "Governança e telemetria<br/>HTTPS" --> pp
    cli & org & ope & adm -. "Login OIDC<br/>HTTPS" .-> idp
    pp -- "Troca e valida tokens<br/>OIDC / JWKS" --> idp
    pp -- "Cobra o pedido<br/>REST + Idempotency-Key" --> pay
    pp -- "Envia comprovante<br/>SMTP" --> mail
    classDef person fill:#08427b,color:#fff,stroke:#052e56
    classDef sys fill:#1168bd,color:#fff,stroke:#0b4884
    classDef ext fill:#8a8a8a,color:#fff,stroke:#5f5f5f
    class cli,org,ope,adm person
    class pp sys
    class idp,pay,mail ext
```

*Figura 1. C1: contexto do PulsePass. Escopo: o sistema inteiro e seus vizinhos.*

| Elemento | Tipo | Pertence ao PulsePass? | Observação |
|---|---|---|---|
| Cliente, Organizador, Operador, Admin | Pessoas | Não | Os quatro papéis RBAC **[Enunciado]** |
| PulsePass | Sistema | Sim | Desenvolvido pelo grupo |
| Provedor de identidade | Externo | Não | Keycloak, produto de terceiros executado localmente **[Decisão do grupo, Pacote 1 (R04)]** |
| Gateway financeiro | Externo | Não (simulado) | Representa um gateway bancário. O grupo escreve um simulador, mas ele fica fora da fronteira lógica do produto **[Decisão do grupo, Pacote 1 (P06)]** |
| Disparo de e-mails | Externo | Não | Em desenvolvimento, Mailpit; em produção seria um provedor SMTP |

## 1.3 Nível 2: Contêineres

O PulsePass adota o estilo arquitetural de **microsserviços** **[Enunciado]**: um serviço implantável por módulo do enunciado, cada um com seu próprio banco lógico, comunicação síncrona (REST) só onde a resposta é necessária na hora e assíncrona (Kafka) nos demais casos (ADR-001).

```mermaid
%% paisagem
flowchart LR
    users["<b>Pessoas</b><br/>Cliente · Organizador<br/>Operador · Admin<br/>[navegador]"]
    subgraph PP["PulsePass"]
      direction LR
      spa["<b>SPA Web</b><br/>[React + TS, Nginx]"]
      gw["<b>API Gateway</b><br/>[Spring Cloud Gateway]<br/>JWT, rotas, rate limit"]
      rt["<b>Realtime Service</b><br/>[WebSocket/STOMP]"]
      iam["<b>IAM Service</b><br/>BFF de login, organizadores,<br/>auditoria"]
      inv["<b>Inventory Service</b><br/>catálogo, reserva,<br/>expiração"]
      chk["<b>Checkout Service</b><br/>pedido, FSM,<br/>pagamento"]
      tkt["<b>Ticket Service</b><br/>ingressos, e-mail,<br/>validação"]
      tel["<b>Telemetry Service</b><br/>ingestão, painéis"]
      rds[("<b>Redis 7</b><br/>sessões, cache,<br/>rate limit, revogação")]
    end
    kc["<b>Keycloak</b><br/>[Externo]"]
    pay["<b>Simulador de<br/>gateway</b><br/>[Externo]"]
    smtp["<b>Mailpit / SMTP</b><br/>[Externo]"]
    users -- "HTTPS" --> spa
    spa -- "REST + JWT" --> gw
    spa -- "WSS + JWT" --> rt
    gw -- "REST" --> iam & inv & chk & tkt & tel
    gw -- "rate limit,<br/>revogação" --> rds
    iam -- "sessões cifradas" --> rds
    inv -- "cache" --> rds
    iam -- "OIDC + PKCE,<br/>Admin API" --> kc
    gw -. "JWKS" .-> kc
    chk -- "reservar / confirmar<br/>REST interno idempotente" --> inv
    chk -- "autorizar / capturar /<br/>cancelar + Idempotency-Key" --> pay
    tkt -- "SMTP" --> smtp
    classDef c fill:#438dd5,color:#fff,stroke:#2e6295
    classDef ext fill:#8a8a8a,color:#fff,stroke:#5f5f5f
    classDef db fill:#c9daf8,color:#111,stroke:#2e6295
    classDef person fill:#08427b,color:#fff
    class spa,gw,rt,iam,inv,chk,tkt,tel c
    class kc,pay,smtp ext
    class rds db
    class users person
```

*Figura 2a. C2: contêineres e chamadas síncronas. Todos os contêineres de backend validam o JWT pela chave pública (JWKS) do Keycloak; só a seta do Gateway aparece para não poluir o diagrama.*

```mermaid
%% paisagem
flowchart LR
    iam["<b>IAM Service</b>"]
    inv["<b>Inventory Service</b>"]
    chk["<b>Checkout Service</b>"]
    tkt["<b>Ticket Service</b>"]
    tel["<b>Telemetry Service</b>"]
    rt["<b>Realtime Service</b>"]
    kfk{{"<b>Cluster Kafka</b><br/>[Apache Kafka, KRaft]<br/>tópicos de domínio + DLQ"}}
    pg[("<b>PostgreSQL 16</b><br/>bancos iam · inv · chk · tkt · tel<br/>(domínio + outbox + inbox)")]
    vol[("<b>Volume de PDFs</b>")]
    iam -- "iam.contas.v1" --> kfk
    inv -- "inventario.catalogo.v1<br/>inventario.reservas.v1" --> kfk
    chk -- "checkout.pedidos.v1" --> kfk
    tkt -- "ingressos.v1" --> kfk
    tel -- "telemetria.navegacao.v1" --> kfk
    kfk -- "reservas" --> chk
    kfk -- "pedidos" --> inv
    kfk -- "pedidos, catálogo,<br/>ingressos" --> tkt
    kfk -- "todos os tópicos" --> tel
    kfk -- "reservas, pedidos,<br/>ingressos" --> rt
    iam & inv & chk & tkt & tel -- "JDBC, banco próprio" --> pg
    tkt -- "grava / lê" --> vol
    classDef c fill:#438dd5,color:#fff,stroke:#2e6295
    classDef db fill:#c9daf8,color:#111,stroke:#2e6295
    class iam,inv,chk,tkt,tel,rt c
    class kfk,pg,vol db
```

*Figura 2b. C2: dados e mensageria. Os serviços que publicam (à esquerda) usam a Outbox no próprio banco; os que consomem usam a Inbox (documento 3). Observabilidade e Kafka UI estão na Figura 3.*

```mermaid
flowchart LR
    subgraph APPS["Todos os serviços Spring Boot e o Gateway"]
      s["/actuator/prometheus<br/>OTLP traces e logs JSON"]
    end
    otel["<b>OpenTelemetry Collector</b>"]
    prom[("<b>Prometheus</b><br/>métricas")]
    jae[("<b>Jaeger</b><br/>traces")]
    graf["<b>Grafana</b><br/>painéis provisionados"]
    kui["<b>Kafka UI</b><br/>[kafbat]"]
    kfk{{"Kafka"}}
    adm["Admin / Grupo"]
    s -- "OTLP gRPC" --> otel --> jae
    prom -- "scrape HTTP 15 s" --> s
    graf -- "PromQL" --> prom
    graf -- "consulta traces" --> jae
    kui -- "admin API" --> kfk
    adm -- "HTTPS (rede de operação)" --> graf & kui
    classDef c fill:#438dd5,color:#fff
    classDef ext fill:#8a8a8a,color:#fff
    class s c
    class otel,prom,jae,graf,kui ext
```

*Figura 3. C2 (complemento): contêineres de operação e observabilidade.*

### 1.3.1 Responsabilidade de cada contêiner

| Contêiner | Módulo do enunciado | Tecnologia | Responsabilidade | Dados que possui | Rótulo |
|---|---|---|---|---|---|
| SPA Web | Todos (front-end) | React + TS (Vite), servida por Nginx | Telas dos 4 perfis (Pacote 1, item 5); guarda só o access token em memória | Nenhum persistente | [Decisão do grupo, Pacote 1 (P02)] |
| API Gateway | Transversal | Spring Cloud Gateway | Ponto único de entrada HTTP; valida JWT; consulta lista de revogação; *rate limiting* por usuário/IP (RNF11); propaga `traceparent` | Contadores no Redis | [Decisão do grupo, Pacote 1] |
| IAM Service | 1. Identidade e Governança | Spring Boot 3, Java 21 | BFF de autenticação (login, refresh, logout), solicitação e aprovação de organizadores, gestão de papéis via Admin API do Keycloak, trilha de auditoria | Banco `iam`; sessões no Redis | [Proposta para revisão do grupo] (ADR-004) |
| Inventory Service | 2. Inventário e Concorrência | Spring Boot 3 | Eventos, setores, lotes, assentos, catálogo de alta leitura, reserva atômica, confirmação, devolução e expiração | Banco `inv`; cache no Redis | [Enunciado] + ADR-002 |
| Checkout Service | 3. Transações e Checkout | Spring Boot 3, Resilience4j | Pedido e FSM, Idempotency-Key, integração com simulador, Outbox, reconciliação | Banco `chk` | [Enunciado] + ADR-003 |
| Ticket Service | 4. Mensageria e Despacho | Spring Boot 3, OpenPDF, ZXing | Consome pagamentos, emite ingressos (PDF + QR assinado), envia e-mail, valida check-in | Banco `tkt`; volume de PDFs | [Enunciado] |
| Realtime Service | 4. Mensageria e Despacho | Spring Boot 3, STOMP | Mantém conexões WebSocket e empurra mudanças de assentos, pedidos e ingressos | Nenhum persistente | [Proposta para revisão do grupo] (ADR-001) |
| Telemetry Service | 5. Telemetria e DevOps | Spring Boot 3, Micrometer | Recebe eventos de navegação da SPA, consome eventos de domínio, agrega funil e vendas, expõe métricas de negócio | Banco `tel` | [Enunciado] |
| PostgreSQL | Infra | PostgreSQL 16 | Fonte da verdade de cada serviço, Outbox e Inbox | 5 bancos + banco `keycloak` | [Decisão do grupo, Pacote 1 (P03)] |
| Redis | Infra | Redis 7 (AOF ligado) | Sessões de refresh (cifradas), cache, *rate limit*, lista de revogação. **Não decide posse de assento** | Dados reconstruíveis, exceto sessões | [Proposta para revisão do grupo] (ADR-002, ADR-004) |
| Cluster Kafka | Infra | Apache Kafka 3.8 (KRaft) | Streaming de eventos de domínio. No Compose local o cluster tem 1 nó; o desenho de tópicos e produtores vale para 3 nós sem mudança de código (documento 3, seção 3.2) | Tópicos (documento 3) | [Enunciado] |
| Kafka UI | Infra | kafbat/kafka-ui | Inspeção de tópicos, grupos, lag e DLQ | Nenhum | [Decisão do grupo, Pacote 1 (P04)] |
| Simulador de gateway | Externo | Spring Boot (código do grupo) | Autorizar, capturar, cancelar; modos de falha e atraso configuráveis | Banco em memória | [Decisão do grupo, Pacote 1 (P06)] |

### 1.3.2 Protocolos e fronteiras de confiança

| Fronteira | De → Para | Protocolo | Autenticação | Observação |
|---|---|---|---|---|
| Internet → borda | Navegador → SPA, Gateway, Realtime | HTTPS / WSS (TLS no Nginx de borda) | JWT Bearer; cookie de sessão `HttpOnly` só no `/auth/*` | Única fronteira exposta (RNF23) |
| Borda → serviços | Gateway → serviços | HTTP na rede Docker interna `backend` | JWT repassado; cada serviço revalida assinatura, `exp`, `aud` e papel (RNF18) | Serviços não publicam portas para fora no Compose |
| Serviço → serviço | Checkout → Inventory | HTTP interno | Token de *client credentials* do Keycloak (conta de serviço `checkout`, escopo `inventario.reservas`) | Endpoints `/internal/*` não passam pelo Gateway |
| Serviço → externo | Checkout → simulador; Ticket → SMTP | HTTP / SMTP | Chave de API do simulador via variável de ambiente | Nunca enviar dados de cartão reais (são simulados) |
| Serviço → infraestrutura | Serviços → PostgreSQL, Redis, Kafka | JDBC, RESP, Kafka | Usuário e senha por serviço (variáveis de ambiente, `.env` fora do Git); ACL Redis por prefixo | Usuário de banco só enxerga o próprio banco (ADR-001) |
| Operação | Grupo → Grafana, Kafka UI, Jaeger | HTTP em rede `ops` | Login do Grafana; Kafka UI só em ambiente local | Não expostos no ambiente de demonstração pública |

### 1.3.3 Principais APIs expostas pelo Gateway

| Serviço | Endpoints (resumo) | Papel exigido |
|---|---|---|
| IAM | `GET /auth/login`, `GET /auth/callback`, `POST /auth/refresh`, `POST /auth/logout`, `POST /organizadores/solicitacoes`, `PATCH /admin/organizadores/{id}`, `PUT /admin/usuarios/{id}/papeis`, `GET /admin/auditoria` | público / autenticado / admin |
| Inventory | `GET /eventos`, `GET /eventos/{id}`, `GET /eventos/{id}/setores/{s}/assentos`, `POST /organizador/eventos`, `POST /organizador/eventos/{id}/lotes`, `PUT .../mapa`, `POST .../publicacao`, `PUT /organizador/eventos/{id}/operadores` | público / organizador dono |
| Checkout | `POST /pedidos` (com `Idempotency-Key`), `GET /pedidos/{id}`, `POST /pedidos/{id}/pagamento` (com `Idempotency-Key`), `POST /pedidos/{id}/cancelamento` | cliente dono |
| Ticket | `GET /ingressos`, `GET /ingressos/{id}/pdf`, `POST /validacoes`, `GET /operador/ingressos?codigo=` | cliente dono / operador vinculado |
| Telemetry | `POST /telemetria/eventos` (lote), `GET /organizador/eventos/{id}/painel`, `GET /admin/painel` | autenticado / organizador dono / admin |

Os contratos detalhados serão gerados em OpenAPI 3 no build de cada serviço (RNF36) a partir do Pacote 3.

## 1.4 Nível 3: Componentes

Todos os serviços seguem **arquitetura hexagonal** **[Decisão do grupo, Pacote 1 (RNF34)]**: adaptadores de entrada (controllers, consumidores) → casos de uso (camada de aplicação) → domínio puro (sem Spring, JPA ou Kafka) → portas implementadas por adaptadores de saída. ArchUnit verifica essas dependências. Os diagramas abaixo mostram os componentes principais; a ordem de prioridade é Inventário, Checkout e Ingressos, que concentram os requisitos de concorrência e consistência.

### 1.4.1 Inventory Service

```mermaid
%% paisagem
flowchart LR
    gw["API Gateway"]
    chk["Checkout Service"]
    subgraph INV["Inventory Service"]
      direction LR
      sec["<b>Filtro de segurança</b><br/>JWT resource server + RBAC<br/>(organizador só nos próprios eventos)"]
      cat["<b>CatalogoController</b><br/>GET eventos e mapa"]
      orgc["<b>OrganizadorEventoController</b><br/>eventos, lotes, mapas, operadores"]
      resc["<b>ReservaInternaController</b><br/>/internal/reservas"]
      ucCat["<b>ConsultarCatalogo</b><br/>caso de uso (cache-aside)"]
      ucEv["<b>GerenciarEvento</b><br/>criar, publicar, encerrar"]
      ucRes["<b>ReservarAssentos</b><br/>tudo ou nada"]
      ucConf["<b>ConfirmarReserva</b><br/>valida expira_em e vende"]
      ucLib["<b>LiberarReserva</b><br/>cancelamento ou expiração"]
      dom["<b>Domínio</b><br/>Evento, Setor, Lote, Assento,<br/>Reserva (regras e estados)"]
      rep["<b>Repositórios</b><br/>EventoRepo, AssentoRepo (UPDATE condicional),<br/>ReservaRepo"]
      cache["<b>CacheCatalogoAdapter</b><br/>Redis"]
      outbox["<b>OutboxWriter</b><br/>insere na mesma transação"]
      sweep["<b>ExpiracaoWorker</b><br/>a cada 1 s, SKIP LOCKED"]
      relay["<b>OutboxRelay</b><br/>líder via advisory lock"]
      cons["<b>PedidoEventosConsumer</b><br/>checkout.pedidos.v1<br/>(PedidoExpirado → libera)"]
    end
    pg[("PostgreSQL inv")]
    rds[("Redis")]
    kfk{{"Kafka"}}
    gw --> sec --> cat & orgc
    chk -- "client credentials" --> resc
    cat --> ucCat --> cache --> rds
    ucCat --> rep
    orgc --> ucEv --> dom
    resc --> ucRes & ucConf & ucLib
    ucRes & ucConf & ucLib --> dom
    ucRes & ucConf & ucLib & ucEv --> rep & outbox
    sweep --> ucLib
    rep & outbox --> pg
    relay -- "lê pendentes" --> pg
    relay -- "produz" --> kfk
    kfk --> cons --> ucLib
    classDef c fill:#85bbf0,color:#000,stroke:#5d82a8
    class sec,cat,orgc,resc,ucCat,ucEv,ucRes,ucConf,ucLib,dom,rep,cache,outbox,sweep,relay,cons c
```

*Figura 4. C3: componentes do Inventory Service.*

| Componente | Responsabilidade | Regra crítica |
|---|---|---|
| `ReservarAssentos` | Recebe `pedidoId`, `eventoId` e 1 a 6 assentos; abre transação; executa o `UPDATE` condicional em ordem de `id`; cria `Reserva` com `expira_em`; grava `ReservaCriada` na Outbox | Linhas afetadas = assentos pedidos, senão *rollback* (RNF01, RNF02, ADR-002). Os **locks concorrentes** exigidos pelo enunciado são os bloqueios de linha que esse `UPDATE` obtém no PostgreSQL, sempre na mesma ordem para evitar *deadlock*. Idempotente por `pedidoId` |
| `ConfirmarReserva` | Chamado pelo Checkout após autorização do pagamento | Só confirma se `estado='ATIVA' AND now() < expira_em`; marca assentos `VENDIDO` (ADR-003) |
| `LiberarReserva` | Cancelamento pelo comprador, falha de pagamento definitiva ou expiração | Devolve todos os assentos da reserva na mesma transação; grava `ReservaExpirada` ou `ReservaLiberada` |
| `ExpiracaoWorker` | Varre reservas vencidas | Várias réplicas podem rodar: `FOR UPDATE SKIP LOCKED` impede duas limpezas da mesma reserva |
| `ConsultarCatalogo` | Leitura de alto volume | *Cache-aside* com TTL de 30 s para catálogo; mapa de assentos com TTL de 2 s e invalidação por evento; o banco continua sendo a verdade |
| `OutboxRelay` | Publica eventos pendentes | Um relay ativo por serviço, em ordem de `id` (ADR-005) |

### 1.4.2 Checkout Service

```mermaid
%% paisagem
flowchart LR
    gw["API Gateway"]
    subgraph CHK["Checkout Service"]
      direction LR
      sec["<b>Filtro de segurança</b><br/>JWT + papel cliente + dono do pedido"]
      idem["<b>IdempotencyFilter</b><br/>exige Idempotency-Key em POST,<br/>hash do corpo, replay da resposta"]
      pc["<b>PedidoController</b>"]
      pgc["<b>PagamentoController</b>"]
      ucCriar["<b>CriarPedidoComReserva</b>"]
      ucPagar["<b>PagarPedido</b><br/>autorizar → confirmar → capturar"]
      ucCanc["<b>CancelarPedido</b>"]
      ucExp["<b>AplicarExpiracao</b>"]
      fsm["<b>Domínio: Pedido + FSM</b><br/>Criado, Reservado, Aguardando,<br/>Pago, Expirado"]
      repo["<b>PedidoRepo, PagamentoRepo,<br/>IdempotenciaRepo</b>"]
      invc["<b>InventarioClient</b><br/>Resilience4j: timeout 2 s,<br/>retry, circuit breaker"]
      payc["<b>GatewayPagamentoClient</b><br/>Resilience4j + Idempotency-Key"]
      outbox["<b>OutboxWriter</b>"]
      relay["<b>OutboxRelay</b>"]
      recon["<b>ReconciliacaoWorker</b><br/>pedidos Aguardando parados > 30 s"]
      cons["<b>ReservaEventosConsumer</b><br/>inventario.reservas.v1<br/>(ReservaExpirada)"]
    end
    inv["Inventory Service"]
    pay["Simulador de gateway"]
    pg[("PostgreSQL chk")]
    kfk{{"Kafka"}}
    gw --> sec --> idem --> pc & pgc
    pc --> ucCriar & ucCanc
    pgc --> ucPagar
    ucCriar & ucPagar & ucCanc & ucExp --> fsm
    ucCriar & ucPagar & ucCanc & ucExp --> repo & outbox
    ucCriar & ucPagar & ucCanc --> invc --> inv
    ucPagar --> payc --> pay
    recon --> ucPagar
    kfk --> cons --> ucExp
    repo & outbox --> pg
    relay -- "lê pendentes" --> pg
    relay -- "produz" --> kfk
    classDef c fill:#85bbf0,color:#000,stroke:#5d82a8
    class sec,idem,pc,pgc,ucCriar,ucPagar,ucCanc,ucExp,fsm,repo,invc,payc,outbox,relay,recon,cons c
```

*Figura 5. C3: componentes do Checkout Service.*

| Componente | Responsabilidade | Regra crítica |
|---|---|---|
| `IdempotencyFilter` | Antes do controller: valida presença da chave (400 se ausente, RNF05), grava registro `EM_ANDAMENTO` com hash SHA-256 do método, rota e corpo | Detalhes na seção 1.5.4 |
| `CriarPedidoComReserva` | Cria pedido `Criado`, chama `ReservarAssentos` no Inventário, avança para `Reservado` com `expiraEm` copiado da reserva | Falha da reserva leva a `Expirado` com motivo `FALHA_RESERVA` |
| `PagarPedido` | `Reservado → Aguardando`, autoriza no simulador, confirma no Inventário, captura, `Aguardando → Pago` + `PagamentoConfirmado` na Outbox | Ordem e compensação do ADR-003 |
| `AplicarExpiracao` | Consome `ReservaExpirada` e leva pedido não pago a `Expirado` | Transição guardada: evento sobre pedido `Pago` é ignorado e registrado (US21) |
| `ReconciliacaoWorker` | Resolve pedidos `Aguardando` após timeouts consultando o simulador e o Inventário | Nunca cria nova cobrança: usa sempre o mesmo `pagamentoId` |
| Domínio FSM | Puro Java, sem framework | Cobertura ≥ 90% de ramos, PIT ≥ 70% (RNF04, RNF32) |

### 1.4.3 Ticket Service e Realtime Service

```mermaid
%% paisagem
flowchart LR
    gw["API Gateway"]
    subgraph TKT["Ticket Service"]
      direction LR
      sec["<b>Filtro de segurança</b><br/>cliente dono / operador vinculado"]
      ingc["<b>IngressoController</b><br/>lista e baixa PDF"]
      valc["<b>ValidacaoController</b><br/>check-in por QR"]
      consPg["<b>PagamentoConfirmadoConsumer</b><br/>grupo tkt-emissao"]
      consCat["<b>CatalogoConsumer</b><br/>projeção evento e operadores"]
      consMail["<b>IngressoEmitidoConsumer</b><br/>grupo tkt-email"]
      inbox["<b>InboxGuard</b><br/>(grupo, eventId) na mesma transação"]
      ucEmit["<b>RegistrarEmissao</b><br/>1 ingresso por assento + jobs"]
      jobs["<b>PdfJobWorker</b><br/>gera PDF e QR, 3 tentativas"]
      ucVal["<b>ValidarIngresso</b><br/>assinatura + uso único"]
      sign["<b>AssinaturaQr</b><br/>Ed25519"]
      pdf["<b>GeradorPdf</b><br/>OpenPDF + ZXing"]
      smtpA["<b>EmailAdapter</b><br/>SMTP, Message-ID estável"]
      repo["<b>IngressoRepo, JobRepo,<br/>ValidacaoRepo</b>"]
      outbox["<b>OutboxWriter + Relay</b>"]
    end
    subgraph RT["Realtime Service"]
      direction LR
      ws["<b>WebSocketEndpoint</b><br/>STOMP; JWT no CONNECT"]
      auth["<b>ChannelInterceptor</b><br/>autoriza SUBSCRIBE por canal"]
      fan["<b>DomainEventFanout</b><br/>grupo exclusivo por instância"]
      sess["<b>SessionRegistry</b><br/>sessões locais desta instância"]
    end
    pg[("PostgreSQL tkt")]
    vol[("Volume de PDFs")]
    kfk{{"Kafka"}}
    smtp["SMTP"]
    spa["SPA"]
    gw --> sec --> ingc & valc
    ingc --> repo
    ingc --> vol
    valc --> ucVal --> sign
    ucVal --> repo & outbox
    kfk --> consPg --> inbox --> ucEmit --> repo
    kfk --> consCat --> repo
    jobs --> pdf --> sign
    jobs --> vol
    jobs --> repo & outbox
    kfk --> consMail --> smtpA --> smtp
    repo & outbox --> pg
    outbox -- "produz ingressos.v1" --> kfk
    spa -- "WSS" --> ws --> auth --> sess
    kfk --> fan --> sess
    classDef c fill:#85bbf0,color:#000,stroke:#5d82a8
    class sec,ingc,valc,consPg,consCat,consMail,inbox,ucEmit,jobs,ucVal,sign,pdf,smtpA,repo,outbox,ws,auth,fan,sess c
```

*Figura 6. C3: componentes do Ticket Service e do Realtime Service.*

| Componente | Responsabilidade | Regra crítica |
|---|---|---|
| `PagamentoConfirmadoConsumer` + `InboxGuard` + `RegistrarEmissao` | Em uma transação: grava `(tkt-emissao, eventId)`, cria um ingresso `PENDENTE` por item de pedido e um job de PDF por ingresso | `UNIQUE (item_pedido_id)` garante um ingresso por assento mesmo com reprocessamento (RNF07) |
| `PdfJobWorker` | Gera o PDF (evento, setor, assento, titular, carimbo de tempo, QR assinado), grava em `ingressos/{ingressoId}.pdf`, marca `EMITIDO` e grava `IngressoEmitido` na Outbox | Mesmo `ingressoId` sobrescreve o mesmo arquivo; após 3 falhas o job vai a `FALHOU` e gera alerta (US25) |
| `ValidarIngresso` | Verifica assinatura, evento e vínculo do operador; `UPDATE ingresso SET estado='UTILIZADO' WHERE id=? AND estado='EMITIDO'` | Linha afetada = liberado; zero linhas = "já utilizado" ou inválido (RNF22, US29) |
| `IngressoEmitidoConsumer` + `EmailAdapter` | Envia e-mail com PDF anexado | Registra `NOTIFICACAO` como PENDENTE antes do SMTP e ENVIADA depois; limite de idempotência no documento 3, seção 3.7. O **disparo multicanal** passa por uma porta `CanalNotificacao`: e-mail (este adaptador) e tela (Realtime, WebSocket) no escopo atual; SMS e push entram como novos adaptadores sem mudar o caso de uso (US27) |
| `DomainEventFanout` | Cada instância do Realtime usa um consumer group próprio (`rt-{instanceId}`), lendo do fim do tópico | Todas as instâncias recebem todos os eventos e entregam só às suas sessões (seção 1.6) |

### 1.4.4 IAM Service

```mermaid
%% paisagem
flowchart LR
    gw["API Gateway"]
    kc["Keycloak"]
    subgraph IAM["IAM Service"]
      direction LR
      authc["<b>AuthController</b><br/>login, callback, refresh, logout"]
      orgc["<b>OrganizadorController</b><br/>solicitar, aprovar, bloquear"]
      admc["<b>AdminController</b><br/>papéis, auditoria"]
      ucSess["<b>GerenciarSessao</b><br/>PKCE, rotação, reuso, revogação"]
      ucOrg["<b>CredenciarOrganizador</b>"]
      ucPap["<b>AlterarPapeis</b>"]
      aud["<b>RegistrarAuditoria</b><br/>append-only"]
      kca["<b>KeycloakAdapter</b><br/>OIDC + Admin REST API"]
      sessA["<b>SessaoRedisAdapter</b><br/>AES-GCM, TTL 8 h"]
      kcev["<b>KeycloakEventsPoller</b><br/>eventos de login/falha"]
      repo["<b>OrganizadorRepo, AuditoriaRepo</b>"]
      outbox["<b>OutboxWriter + Relay</b>"]
    end
    rds[("Redis")]
    pg[("PostgreSQL iam")]
    kfk{{"Kafka"}}
    gw --> authc & orgc & admc
    authc --> ucSess --> kca & sessA
    orgc --> ucOrg --> kca
    admc --> ucPap --> kca
    ucSess & ucOrg & ucPap & kcev --> aud --> repo
    ucOrg & ucPap --> outbox
    kca --> kc
    kcev --> kc
    sessA --> rds
    repo & outbox --> pg
    outbox -- "iam.contas.v1" --> kfk
    classDef c fill:#85bbf0,color:#000,stroke:#5d82a8
    class authc,orgc,admc,ucSess,ucOrg,ucPap,aud,kca,sessA,kcev,repo,outbox c
```

*Figura 7. C3: componentes do IAM Service (ADR-004).*

### 1.4.5 Telemetry Service

```mermaid
flowchart TB
    spa["SPA"]
    gw["API Gateway"]
    subgraph TEL["Telemetry Service"]
      direction TB
      ingC["<b>IngestaoController</b><br/>POST /telemetria/eventos (lote)"]
      prod["<b>NavegacaoProducer</b><br/>telemetria.navegacao.v1"]
      cons["<b>DomainEventsConsumer</b><br/>grupo tel-agregador"]
      agg["<b>AgregadorJanela</b><br/>buckets de 1 min por evento"]
      painel["<b>PainelController</b><br/>organizador dono / admin"]
      met["<b>MetricasNegocio</b><br/>Micrometer gauges e counters"]
      repo["<b>AgregadoRepo + Inbox</b>"]
    end
    pg[("PostgreSQL tel")]
    kfk{{"Kafka"}}
    prom[("Prometheus")]
    spa --> gw --> ingC --> prod --> kfk
    kfk --> cons --> agg --> repo --> pg
    agg --> met
    prom -- "scrape" --> met
    gw --> painel --> repo
    classDef c fill:#85bbf0,color:#000,stroke:#5d82a8
    class ingC,prod,cons,agg,painel,met,repo c
```

*Figura 8. C3: componentes do Telemetry Service.*

O Telemetry Service consome os tópicos de domínio e o de navegação no grupo `tel-agregador`, separado dos grupos de Checkout e Ingressos, de modo que todos recebem os mesmos eventos sem competir (documento 3). O painel do organizador filtra pelos eventos do organizador usando a projeção `evento → organizador` recebida de `inventario.catalogo.v1`.

## 1.5 Fluxos críticos (diagramas de sequência)

### 1.5.1 Reserva concorrente do mesmo assento

```mermaid
sequenceDiagram
    autonumber
    participant A as Comprador A
    participant B as Comprador B
    participant C as Checkout
    participant I as Inventory
    participant DB as PostgreSQL inv
    A->>C: POST /pedidos {assentos:[S1,S2]} + Idempotency-Key kA
    B->>C: POST /pedidos {assentos:[S2]} + Idempotency-Key kB
    C->>I: POST /internal/reservas (pedido PA)
    C->>I: POST /internal/reservas (pedido PB)
    I->>DB: BEGIN · UPDATE assento ... WHERE id IN (S1,S2) AND estado='DISPONIVEL' (ordem por id)
    I->>DB: BEGIN · UPDATE assento ... WHERE id IN (S2) AND estado='DISPONIVEL'
    Note over DB: A trava S1 e S2 primeiro. B espera o bloqueio da linha S2.
    DB-->>I: A: 2 linhas afetadas (= 2 pedidas)
    I->>DB: INSERT reserva (expira_em = now()+600s) + INSERT outbox ReservaCriada · COMMIT
    DB-->>I: B: 0 linhas afetadas (S2 já RESERVADO)
    I->>DB: ROLLBACK
    I-->>C: A: 201 {reservaId, expiraEm}
    I-->>C: B: 409 ASSENTO_INDISPONIVEL
    C-->>A: 201 pedido Reservado + contagem regressiva
    C-->>B: 409 "Assento indisponível, escolha outro" (pedido Expirado, motivo FALHA_RESERVA)
```

*Figura 9. Dois compradores disputando S2. Com 1.000 compradores o comportamento é o mesmo: uma única transação vence (RNF01, US18).*

### 1.5.2 Pagamento, Outbox, Kafka e emissão do ingresso

```mermaid
sequenceDiagram
    autonumber
    participant S as SPA
    participant C as Checkout
    participant P as Simulador
    participant I as Inventory
    participant CDB as PostgreSQL chk
    participant K as Kafka
    participant T as Ticket
    participant R as Realtime
    S->>C: POST /pedidos/{id}/pagamento + Idempotency-Key k1
    C->>CDB: idempotência k1 EM_ANDAMENTO · pedido Reservado→Aguardando · COMMIT
    C->>P: POST /autorizacoes (Idempotency-Key = pagamentoId)
    P-->>C: AUTORIZADO
    C->>I: POST /internal/reservas/{r}/confirmacao (pedidoId)
    I-->>C: 200 CONFIRMADA (assentos VENDIDO)
    C->>P: POST /autorizacoes/{id}/captura
    P-->>C: CAPTURADO
    C->>CDB: BEGIN · pedido Aguardando→Pago · INSERT outbox PagamentoConfirmado · idempotência k1 CONCLUIDA · COMMIT
    C-->>S: 200 {estado: Pago}
    loop OutboxRelay (líder)
      C->>CDB: SELECT pendentes ORDER BY id
      C->>K: produce checkout.pedidos.v1 (key=pedidoId, acks=all)
      K-->>C: ack
      C->>CDB: UPDATE outbox SET publicado_em=now()
    end
    K->>T: PagamentoConfirmado (grupo tkt-emissao)
    T->>T: tx: inbox + ingressos PENDENTE + jobs · commit · ack offset
    T->>T: PdfJobWorker gera PDF + QR · tx: EMITIDO + outbox IngressoEmitido
    K->>R: PedidoPago / IngressoEmitido (grupo rt-instancia)
    R-->>S: STOMP /user/queue/pedidos "Ingresso emitido"
```

*Figura 10. Caminho feliz do pagamento até o aviso em tempo real (US22, US23, US25, US28, RNF12).*

### 1.5.3 Expiração concorrendo com a confirmação

```mermaid
sequenceDiagram
    autonumber
    participant C as Checkout
    participant P as Simulador
    participant I as Inventory
    participant W as ExpiracaoWorker
    participant DB as PostgreSQL inv
    Note over C,DB: Reserva R criada às 20:00:00, expira_em = 20:10:00
    C->>P: autorizar (20:09:58)
    P-->>C: AUTORIZADO (20:09:59.6)
    par Corrida
      C->>I: confirmar R (chega 20:10:00.2)
      I->>DB: UPDATE reserva SET estado='CONFIRMADA' WHERE id=R AND estado='ATIVA' AND now() < expira_em
      DB-->>I: 0 linhas (now() >= expira_em)
      I-->>C: 409 RESERVA_EXPIRADA
    and
      W->>DB: SELECT reservas ATIVA com expira_em <= now() FOR UPDATE SKIP LOCKED
      W->>DB: reserva EXPIRADA, assentos DISPONIVEL, outbox ReservaExpirada · COMMIT
    end
    C->>P: cancelar autorização (mesmo pagamentoId)
    C->>C: pedido Aguardando→Expirado (PAGAMENTO_TARDIO) + outbox EstornoSolicitado
    Note over C,DB: Se a confirmação tivesse chegado às 20:09:59.9, o UPDATE afetaria 1 linha,<br/>os assentos virariam VENDIDO e o worker não encontraria a reserva ATIVA.
```

*Figura 11. A linha condicional no banco do Inventário decide sozinha quem vence. Pagamento tardio nunca volta para Pago (ADR-003, US19, US21).*

### 1.5.4 Repetição HTTP e recuperação após falha do broker

```mermaid
sequenceDiagram
    autonumber
    participant S as SPA
    participant C as Checkout
    participant DB as PostgreSQL chk
    participant K as Kafka
    S->>C: POST pagamento + Idempotency-Key k1 (1ª tentativa)
    C->>DB: INSERT idempotencia(usuario, escopo, k1, hash, EM_ANDAMENTO) OK
    S->>C: POST pagamento + k1 (clique duplo, mesmo corpo)
    C->>DB: INSERT ... ON CONFLICT → registro EM_ANDAMENTO, mesmo hash
    C-->>S: 409 EM_PROCESSAMENTO + Retry-After: 1
    S->>C: POST pagamento + k1 (corpo diferente)
    C-->>S: 422 CHAVE_REUTILIZADA_COM_OUTRO_CORPO
    Note over C,K: Kafka indisponível neste momento
    C->>DB: pedido Pago + outbox PagamentoConfirmado + k1 CONCLUIDA (200, corpo) COMMIT
    C-->>S: 200 {estado: Pago}
    C-xK: relay tenta publicar: falha de conexão
    Note over C,DB: Evento continua pendente na Outbox. Nada é perdido (RNF06).
    S->>C: POST pagamento + k1 (reconexão do navegador)
    C->>DB: registro CONCLUIDA com mesmo hash
    C-->>S: 200 {estado: Pago} (resposta original, sem nova cobrança)
    K-->>C: broker volta
    C->>K: relay publica pendentes em ordem de id
    C->>DB: marca publicado_em
    Note over C,K: Se o relay cair entre o ack do Kafka e o UPDATE, o evento é publicado de novo.<br/>O consumidor deduplica por eventId (documento 3, seção 3.6).
```

*Figura 12. Idempotency-Key e Outbox sob repetição e falha do broker (RNF05, RNF06, RNF07, US22, US23).*

**Regras da Idempotency-Key** **[Proposta para revisão do grupo]**, detalhando RNF05 e US22:

| Situação | Resposta |
|---|---|
| POST de escrita sem `Idempotency-Key` | 400 |
| Chave nova | Processa; registro passa de `EM_ANDAMENTO` para `CONCLUIDA` guardando status HTTP e corpo |
| Mesma chave, mesmo hash, `CONCLUIDA` | Devolve a resposta guardada, sem novo efeito |
| Mesma chave, mesmo hash, `EM_ANDAMENTO` | 409 com `Retry-After: 1` (não executa em paralelo) |
| Mesma chave, hash diferente | 422 (definido na US22) |
| `EM_ANDAMENTO` há mais de 30 s (processo caiu) | Não reexecuta às cegas: o pedido é reconciliado pelo estado atual na FSM e no simulador, e a resposta é derivada desse estado |

Escopo da chave: `(usuarioId, metodo + rota, chave)`; duas pessoas podem usar o mesmo UUID sem colisão. Retenção: 24 h (RNF05), removida por job diário.

## 1.6 Tempo real: caminho WebSocket e controle de canais

| Aspecto | Desenho **[Proposta para revisão do grupo]** |
|---|---|
| Conexão | SPA abre `wss://.../ws` no Realtime Service; o *frame* STOMP `CONNECT` leva o access token; o `ChannelInterceptor` valida assinatura, `exp` e papéis |
| Canais | `/topic/eventos/{eventoId}/assentos` (mapa de assentos, qualquer usuário autenticado); `/user/queue/pedidos` (pedidos e ingressos do próprio usuário, destino resolvido pelo `sub` do token); `/topic/organizador/eventos/{eventoId}/vendas` (só o organizador dono, verificado na projeção de eventos) |
| Autorização | Cada `SUBSCRIBE` é checado. Um cliente não consegue assinar a fila de outro usuário porque o destino `/user/...` é sempre resolvido pelo servidor a partir do token |
| Fonte das mensagens | O Realtime não recebe chamadas dos serviços: ele consome `inventario.reservas.v1`, `checkout.pedidos.v1` e `ingressos.v1` e traduz para mensagens de tela |
| Múltiplas instâncias | Cada instância entra em um **consumer group exclusivo** (`rt-{instanceId}`) com `auto.offset.reset=latest`; todas recebem todos os eventos e cada uma entrega apenas às sessões conectadas nela. Não há estado compartilhado entre instâncias |
| Reconexão | O cliente reconecta em até 5 s (RNF29) e, ao reconectar, busca o estado atual por REST (`GET /pedidos/{id}`, mapa de assentos). O WebSocket é otimização; a fonte de verdade continua sendo a API |
| Expiração do token | Sessão WebSocket é encerrada quando o access token vence; a SPA renova pelo `/auth/refresh` e reconecta |

## 1.7 Segurança transversal

| Tema | Decisão |
|---|---|
| Tokens | Access token JWT RS256 do Keycloak, validade ≤ 5 min, validado pelo JWKS em cada contêiner (RNF18). É assim que o Inventário e os demais módulos "validam a identidade com o IAM" [Enunciado]: o módulo IAM é o conjunto IAM Service + Keycloak, e a assinatura criptográfica dos tokens vale para todas as rotas |
| Sessão | Refresh token do Keycloak guardado cifrado no Redis pelo IAM; SPA recebe identificador opaco em cookie `HttpOnly` (ADR-004, RNF19) |
| RBAC | Papéis no realm; anotações `@PreAuthorize` em todo controller e checagem de dono do recurso (organizador do evento, cliente do pedido, operador vinculado) (RNF20) |
| Auditoria | Eventos de credencial gravados *append-only* no banco `iam` (permissão só de `INSERT` e `SELECT` para o usuário da aplicação) (RNF21) |
| Ingresso | QR com `ingressoId`, `eventoId` e assinatura Ed25519; chave privada só no Ticket Service via variável de ambiente (RNF22) |
| Segredos | Nada versionado; `.env.example` sem valores reais; gitleaks no CI (RNF23) |
| Logs | JSON com `traceId`; filtro mascara `Authorization`, cookies e chaves (RNF27) |

## 1.8 Rastreabilidade

| Requisito | Onde é atendido neste documento |
|---|---|
| US18, US19, RNF01, RNF02, RNF03 | 1.4.1, Figuras 9 e 11, ADR-002 |
| US21, US22, US23, US24, RNF04, RNF05, RNF06, RNF15 | 1.4.2, Figuras 10 a 12, seção 1.5.4, ADR-003 |
| US25, US28, US29, RNF07, RNF08, RNF12, RNF22, RNF29 | 1.4.3, Figura 10, seção 1.6 |
| RNF18 a RNF21, RNF23 | 1.3.2, 1.4.4, 1.7, ADR-004 |
| RNF25, RNF26, RNF28 | Figura 3, 1.4.5 |
| RNF34 | Introdução da seção 1.4 (hexagonal + ArchUnit), ADR-001 |
| RNF37 a RNF40, US32, US33 | Contêineres em 1.3; detalhes no documento 4 |
