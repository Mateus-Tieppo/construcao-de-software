# 5. Mockups (medium res) e Mapa de Navegação do Front-end

## 5.1 Fidelidade escolhida e ferramenta

- **Fidelidade: média (*medium-fi*)** — wireframes em tons de cinza com uma única cor de destaque (roxo, ações primárias), dados fictícios realistas e estados de erro/borda anotados. Não representam a identidade visual final.
- **Ferramenta:** HTML + CSS simples (`mockups/*.html` + `mockups/wireframe.css`), capturados em PNG com Chrome *headless* (`mockups/img/`). A escolha permite versionar os mockups no Git, revisá-los em PR e gerá-los/ajustá-los com apoio do agente de IA (ver item 1) **[Decisão do grupo]**.
- **Front-end previsto:** SPA em React + TypeScript, responsiva, com rotas protegidas por papel (RBAC lido do JWT do Keycloak) e canal WebSocket para atualizações em tempo real.
- **Notas amarelas** nos mockups indicam comportamento não visível em imagem estática (ex.: atualização via WebSocket).

## 5.2 Mapa de navegação

### 5.2.1 Visão geral por perfil

Após o login (Keycloak), a SPA redireciona para a tela inicial do papel do usuário. Um usuário com mais de um papel pode alternar pelo menu do perfil.

```mermaid
flowchart LR
    PUB["Catálogo público<br/>(visitante)"] --> LOGIN["T00 Login / Cadastro<br/>(Keycloak)"]
    LOGIN -->|"papel = Cliente"| C1["T01 Catálogo"]
    LOGIN -->|"papel = Organizador"| O1["T06 Painel de vendas"]
    LOGIN -->|"papel = Operador"| P1["T08 Validação de QR"]
    LOGIN -->|"papel = Admin"| A1["T09 Organizadores"]
    LOGIN -.->|"Quero ser organizador"| SOL["Solicitar credenciamento"]
    SOL -.->|"aprovação do Admin"| O1
```

**Comprador**

```mermaid
flowchart LR
    C1["T01 Catálogo"] --> C2["T02 Detalhe<br/>do evento"]
    C2 --> C3["T03 Mapa de<br/>assentos"]
    C3 -->|Reservar| C4["T04 Checkout<br/>(timer 10 min)"]
    C4 -->|Pago| C5["T05 Confirmação /<br/>Meus ingressos"]
    C4 -->|"Expirou / cancelou"| C3
    C4 -->|Recusado| C4
    C1 --> C5
    C1 --> C6["Meus pedidos"]
    C5 --> PDF[/"PDF do ingresso"/]
```

**Organizador**

```mermaid
flowchart LR
    O1["T06 Painel de vendas"] --> O3["Meus eventos"]
    O3 --> O2["T07 Criar/editar evento<br/>dados → lotes → mapa → publicar"]
    O2 -->|Publicar| O1
    O3 --> O4["Operadores do evento"]
```

**Operador**

```mermaid
flowchart LR
    P1["T08 Validação de QR"] --> P2["Resultado: liberado /<br/>já utilizado / inválido"]
    P2 -->|próximo| P1
    P1 --> P3["Consultar ingresso"]
```

**Admin**

```mermaid
flowchart LR
    A1["T09 Organizadores<br/>(aprovar/recusar)"] --> A2["Usuários e papéis"]
    A1 --> A3["T10 Telemetria e saúde"]
    A3 --> A4["Auditoria de credenciais"]
    A2 --> A4
```

### 5.2.2 Fluxo crítico de compra (com estados do pedido)

```mermaid
flowchart TB
    T02["T02 Detalhe"] -->|Escolher assentos| T03["T03 Mapa<br/>(WebSocket)"]
    T03 -->|"Reservar → pedido Criado → Reservado"| T04["T04 Checkout<br/>timer 10:00"]
    T03 -->|Assento indisponível| T03
    T04 -->|"Pagar (Idempotency-Key) → Aguardando"| W{{"Gateway simulado"}}
    W -->|Aprovado → Pago| T05["T05 Confirmação<br/>ingresso via WebSocket"]
    W -->|Recusado → Reservado| T04
    T04 -->|"10 min → Expirado"| EXP["Aviso: reserva expirada"] --> T03
```

### 5.2.3 Rotas da SPA e papéis

| Tela | Rota | Papel exigido | Histórias |
|---|---|---|---|
| T00 Login / Cadastro | `/entrar`, `/cadastro` | público | US01, US02, US05 |
| T01 Catálogo | `/` , `/eventos?busca=` | público | US14, US16 |
| T02 Detalhe do evento | `/eventos/:id` | público | US15 |
| T03 Mapa de assentos | `/eventos/:id/assentos` | Cliente | US17, US18, US20 |
| T04 Checkout | `/pedidos/:id/pagamento` | Cliente | US19, US21, US22, US24 |
| T05 Meus ingressos | `/meus-ingressos` | Cliente | US25, US26, US28 |
| Meus pedidos | `/meus-pedidos` | Cliente | US21 |
| T06 Painel de vendas | `/organizador` | Organizador | US34 |
| T07 Criar/editar evento | `/organizador/eventos/:id/editar` | Organizador | US09–US13 |
| T08 Validação de QR | `/operador/validar` | Operador | US29, US30 |
| T09 Organizadores / usuários | `/admin/organizadores`, `/admin/usuarios` | Admin | US06, US07 |
| T10 Telemetria / auditoria | `/admin/telemetria`, `/admin/auditoria` | Admin | US08, US35 |

Acesso a rota sem o papel exigido → tela "Acesso negado" (HTTP 403); sem sessão → redireciona para `/entrar` e retorna à rota original após login.

## 5.3 Telas — Comprador

**T00 — Login** (compartilhada por todos os perfis; autenticação OIDC no Keycloak; estado de erro de credenciais).<br>
![T00 Login](mockups/img/00-login.png)

**T01 — Catálogo de eventos** (busca, filtros, indicadores "Esgotado"/"Últimos"/"Abre em").<br>
![T01 Catálogo](mockups/img/01-comprador-catalogo.png)

**T02 — Detalhe do evento** (lotes à venda, limite por pedido).<br>
![T02 Detalhe do evento](mockups/img/02-comprador-evento.png)

**T03 — Mapa de assentos** (legenda de status, seleção, aviso de bloqueio de 10 min, erro de assento disputado).<br>
![T03 Mapa de assentos](mockups/img/03-comprador-assentos.png)

**T04 — Checkout** (contagem regressiva, status da FSM, botão com Idempotency-Key, estado de reserva expirada).<br>
![T04 Checkout](mockups/img/04-comprador-checkout.png)

**T05 — Confirmação e Meus ingressos** (QR Code, carimbo de tempo, download do PDF, aviso em tempo real).<br>
![T05 Meus ingressos](mockups/img/05-comprador-ingressos.png)

## 5.4 Telas — Organizador

**T06 — Painel de vendas em tempo real** (KPIs, vendas por minuto, lotes).<br>
![T06 Painel do organizador](mockups/img/06-organizador-painel.png)

**T07 — Criar evento** (assistente: dados → lotes → mapa de assentos → publicar).<br>
![T07 Criar evento](mockups/img/07-organizador-evento.png)

## 5.5 Telas — Operador

**T08 — Validação de acesso por QR Code** (câmera do navegador, entrada manual, três resultados possíveis).<br>
![T08 Validação](mockups/img/08-operador-validacao.png)

## 5.6 Telas — Admin

**T09 — Organizadores e usuários/papéis** (aprovação de credenciamento, RBAC, bloqueio).<br>
![T09 Organizadores](mockups/img/09-admin-organizadores.png)

**T10 — Telemetria, saúde dos serviços e auditoria de credenciais.**<br>
![T10 Telemetria](mockups/img/10-admin-telemetria.png)
