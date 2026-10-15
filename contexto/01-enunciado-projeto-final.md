# Enunciado — Projeto Integrador de Construção de Software (2026/2)

> Transcrição do PDF `Projeto_Final_Construcao_Software-2026-2.pdf` (Moodle PUCRS, disciplina 46504-04, Prof. Michael da Costa Mora).
> As tabelas do PDF foram reorganizadas em formato Markdown; o conteúdo foi mantido.

PONTIFÍCIA UNIVERSIDADE CATÓLICA DO RIO GRANDE DO SUL — ESCOLA POLITÉCNICA
Construção de Software – 2026/2
**PROJETO INTEGRADOR DE CONSTRUÇÃO DE SOFTWARE**

## Contextualização do Domínio de Negócios

Um dos objetivos da disciplina é a consolidação de competências que transcendem o desenvolvimento de sistemas transacionais convencionais baseados em operações puramente síncronas. Em ambientes reais de alta concorrência, sistemas estruturados exclusivamente sobre HTTP síncrono enfrentam gargalos severos de esgotamento de conexões de banco de dados e acoplamento temporal prejudicial à resiliência.

Assim, o domínio adotado para este projeto é uma **Plataforma Distribuída de Bilhetagem, Reserva e Acesso para Megaeventos** ("PulsePass"? Sugestões de nomes são bem-vindas!), sujeita a tráfego massivo em picos (ou rajadas — *flash crowd traffic*) no instante de abertura de vendas. O sistema exige **tolerância zero à sobre-venda (zero overselling)**, **expiração estrita de reservas temporárias em 10 minutos** e **emissão segura de ingressos**.

## Requisitos Funcionais

O escopo é estruturado em cinco frentes funcionais autônomas:

| Foco | Domínio de Responsabilidade | Requisitos Funcionais Principais | Complexidade | Integração Técnica |
|---|---|---|---|---|
| **Identidade e Governança** | Módulo de Identidade, Autenticação e Autorização (IAM) | • Autenticação stateless via JWT com refresh tokens em chave-valor.<br>• Autorização granular baseada em papéis (RBAC): Cliente, Organizador, Operador e Admin.<br>• Auditoria de credenciais e gestão cadastral de organizadores de eventos. | Média | Provê mecanismos transversais de segurança e assinatura criptográfica de tokens para todas as rotas. |
| **Inventário e Concorrência** | Módulo de Catálogo, Assentos e Bloqueio Concorrente | • Criação de lotes e mapas de assentos.<br>• Catálogo de eventos de alta leitura.<br>• Alocação atômica de assentos.<br>• Devolução atômica de assentos em cancelamento ou timeout. | Alta | Valida identidade com o IAM; emite locks concorrentes e publica eventos de expiração de reserva no broker. |
| **Transações e Checkout** | Módulo de Pedidos, Checkout e Transações Resilientes | • Máquina de estados finitos do pedido (Criado, Reservado, Aguardando, Pago, Expirado).<br>• Suporte nativo a Idempotency-Key em cabeçalhos HTTP.<br>• Aplicação do padrão Transactional Outbox em transação ACID relacional para publicação segura no Kafka. | Alta | Consome reservas do Inventário; integra simulador de gateway financeiro; despacha eventos atômicos ao Kafka. |
| **Mensageria e Despacho** | Módulo de Emissão de Ingressos, PDF e Notificações | • Consumo assíncrono de eventos de pagamentos confirmados.<br>• Geração de ingressos em PDF com carimbo de tempo e QR Code.<br>• Disparo multicanal de comprovantes e atualização de tela em tempo real. | Média-Alta | Monitora tópicos de pagamentos concluídos. |
| **Análise e Operações** | Módulo de Telemetria e DevOps | • Ingestão e agregação contínua de eventos de navegação e compras.<br>• Painel analítico de métricas em tempo real.<br>• Pipeline de CI/CD automatizada e orquestração Docker Compose. | Alta | Consome tópicos de domínio da plataforma; coleta métricas operacionais e automatiza a governança de código. |

## Aspectos de Arquitetura e Design de Sistemas

O desenvolvimento do trabalho deve contemplar os seguintes aspectos:

- Desenvolver a solução utilizando o estilo arquitetural baseado em **microsserviços**;
- Utilizar um provedor de identificação e autenticação como **Keycloak ou Cognito**;
- Utilizar **Kafka** como provedor de streaming em tempo real;
- Garantir que problemas como **escrita dupla (Dual-Write Dilemma)** e **idempotência no consumo** são adequadamente tratados;
- **Documentação do Processo de Desenvolvimento de Software utilizado**
  - Deve ser suportado por IA;
  - Ciclo de desenvolvimento, Ferramentas, agentes e specs utilizadas;
- **Documentação Arquitetural utilizando o Modelo C4**
  - Nível 1 – Contexto: Fronteiras de software, perfis de usuários (comprador, organizador, operador) e sistemas externos (gateways bancários, disparo de emails).
  - Nível 2 – Contêineres: Distribuição física entre Single Page Application (SPA), APIs backend, bancos (PostgreSQL/Redis, ...), cluster Kafka e servidores WebSocket.
  - Nível 3 – Componentes: Estrutura interna dos controladores de borda, casos de uso, repositórios, workers do Outbox e consumidores de mensageria.
- **Pirâmide de Testes e Reprodutibilidade DevOps**
  - Testes Automatizados: Testes unitários cobrindo máquinas de estado (>= 70% de cobertura de domínio); testes de integração utilizando Testcontainers reais de PostgreSQL e Kafka (e Redis, se for o caso). Testes de carga seriam um bônus desejável;
  - Definição e descrição do processo de GCS (Gerenciamento da Configuração de Software): ferramenta, descrição do fluxo de trabalho (branchs, regras de uso, ...);
  - Conteinerização e CI/CD: Ambiente completo orquestrado via docker-compose.yml (backend, frontend, bancos, Kafka e UI de mensageria). Pipeline de CI automatizada no GitHub Actions/GitLab executando linters, testes e compilação a cada Pull Request ou Merge na Branch "develop".

## Organização

### Grupos
O trabalho deve ser desenvolvido em grupos de 3 até 5 pessoas.

### Entregáveis
O trabalho será dividido em quatro pacotes de entregáveis:

- **Pacote 1 – Processo de Desenvolvimento e Requisitos**
  - Descrição do processo de desenvolvimento suportado por IA, como descrito no enunciado;
  - Documento de Visão do Produto;
  - Backlog com Épicos e Histórias de usuários, no formato de Story Mapping;
  - Requisitos não-funcionais;
  - Mockups (low, medium ou high res, à escolha) e mapa de navegação do front-end;
- **Pacote 2 – Arquitetura e Infraestrutura de Desenvolvimento**
  - Descrição da arquitetura no modelo C4 descrito no enunciado;
  - Descrição do Modelo de Dados Conceitual;
  - Descrição da estrutura de mensageria do Kafka;
  - Descrição esquemática da Pipeline de desenvolvimento;
  - Descrição do Processo de GCS;
- **Pacote 3 – Checkpoint do Desenvolvimento**
  - Link para o repositório com o estágio corrente de desenvolvimento;
  - Documentação entregue nos pacotes 1 e 2 atualizada;
- **Pacote 4 – Produto Completo**
  - Link para o repositório com o produto completo;
  - Documentação entregue nos pacotes 1 e 2 atualizada.

### Cronograma
Disponível no Moodle (ver `02-disciplina-moodle.md`).
