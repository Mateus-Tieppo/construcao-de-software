# ADR-001: Fronteiras dos microsserviços e propriedade de dados

**Estado:** Aceita pelo grupo em 04/11/2026

## Contexto
O enunciado define cinco frentes funcionais autônomas e exige microsserviços. O Pacote 1 adotou *Database per Service* (P03) e arquitetura hexagonal (RNF34), mas não definiu quantos processos implantáveis existem nem quem é dono de cada dado.

## Decisão
- Um serviço Spring Boot por módulo: `iam-service`, `inventory-service`, `checkout-service`, `ticket-service`, `telemetry-service`.
- Dois processos de borda: `api-gateway` (Spring Cloud Gateway) e `realtime-service` (WebSocket/STOMP), este último pertencente ao módulo 4 (Mensageria e Despacho).
- Um contêiner PostgreSQL com **um banco lógico e um usuário por serviço**; cada usuário só tem privilégio no próprio banco. Keycloak tem banco próprio.
- Referências entre serviços são identificadores (UUID) e contratos (REST/eventos). Proibido FK ou *join* entre bancos de serviços diferentes.

## Alternativas
1. Um contêiner PostgreSQL por serviço: isolamento físico maior, custo de memória alto no notebook de referência (RNF37).
2. Juntar Ingressos e WebSocket em um processo: menos peças, mas o WebSocket escala por conexões e o gerador de PDF por CPU.
3. Monólito modular: contraria o enunciado.

## Consequências
- Consistência entre serviços é eventual e passa por eventos (ADR-005) ou por chamadas síncronas idempotentes (ADR-003).
- ArchUnit e permissões de banco tornam a fronteira verificável (RNF34).
- Mais contêineres no Compose; mitigado com limites de memória por serviço.
