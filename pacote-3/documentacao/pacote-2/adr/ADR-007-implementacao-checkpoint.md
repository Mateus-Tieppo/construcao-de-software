# ADR-007: Decisões de implementação do checkpoint

- **Estado:** Proposta para revisão do grupo (Pacote 3, 23/11/2026)
- **Relacionados:** ADR-001, ADR-002, ADR-005

## Contexto
Ao transformar o desenho do Pacote 2 em código surgiram escolhas que os ADRs anteriores não cobriam: tecnologia de persistência, onde ficam Outbox e Inbox, como as imagens são construídas e como os tópicos são criados nos testes.

## Decisão
1. **Persistência com `JdbcTemplate` e SQL explícito**, sem JPA. As garantias de concorrência dependem de `UPDATE ... WHERE estado = ?` com contagem de linhas, `FOR UPDATE SKIP LOCKED` e `pg_try_advisory_xact_lock`; com SQL explícito elas ficam visíveis na revisão.
2. **Biblioteca `platform-commons`** com envelope de eventos, `OutboxWriter`, `OutboxRelay`, `InboxGuard`, DLQ, catálogo de tópicos e conversão de papéis do Keycloak. É código compartilhado em tempo de compilação; cada serviço continua com o próprio banco e as próprias tabelas `outbox` e `mensagem_processada` (ADR-001 mantido).
3. **Tópicos criados de forma idempotente** pelo `kafka-init` no Compose e pelo `KafkaAdmin` de cada serviço, que também cobre os testes com Testcontainers.
4. **Imagem de execução única** (`services/Dockerfile`) que copia o JAR compilado pelo Maven; a CI compila antes do `docker compose up --build`.

## Alternativas consideradas
- JPA com `@Version`: resolveria o *lock* otimista, mas esconderia o `UPDATE` condicional e exigiria consultas nativas para `SKIP LOCKED` e *advisory lock* de qualquer forma.
- Copiar Outbox e Inbox em cada serviço: mais isolamento de código, mas três cópias da mesma lógica crítica para manter.
- Build multi-estágio com Maven dentro de cada imagem: não exige JDK na máquina, mas compila o reator inteiro uma vez por serviço.

## Consequências
- Mudança em `platform-commons` afeta todos os serviços e precisa de revisão mais cuidadosa.
- Quem sobe o ambiente precisa de JDK 21 e Maven, ou da imagem `maven:3.9-eclipse-temurin-21`, para compilar antes.
