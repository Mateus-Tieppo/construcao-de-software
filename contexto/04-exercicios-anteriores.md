# Exercícios anteriores da disciplina (contexto técnico)

Úteis para saber o que o professor já cobrou e qual stack a turma domina. Não fazem parte do Pacote 1, mas o TF deve ser coerente com eles.

## Aula 03 — Review de Arquitetura (questões)
Arquitetura de software e visões (Hanmer); estilos (Camadas, Serverless); padrões arquiteturais; impacto de microsserviços no deploy e no armazenamento; microfrontends; BFF; padrões Gateway, Circuit Breaker, Saga.

## Aulas 04–05 — API em microsserviços
Estudantes (cadastrar; buscar por matrícula e por parte do nome), disciplinas (código, nome, horários A–G; mesma disciplina em vários horários), matrícula. Escolher a divisão em serviços, rotas e request/response. BD podia ser centralizado. Entrega: link do repositório no fórum.

## Aula 06 — Padrões de Microsserviços (apresentação)
Para cada padrão: contexto, problema, forças, solução (diagrama), exemplos, contexto resultante, padrões relacionados, código.
Grupos: API Gateway + API Composition; Client/Server-side Service Discovery; Saga + Database per Service; Circuit Breaker + Bulkhead; CQRS.

## Aulas 08/10 — Spring Cloud
Sistema de Peças, Clientes e Representantes Comerciais (cadastro, consulta por nome/id/CPF, listagem) em ≥3 microsserviços com **Gateway, Service Discovery e Configuração Centralizada (Spring Cloud)**; testar só via Gateway. Grupos até 4, "use seu processo e ferramentas de desenvolvimento com IA, se possível".

## Aula 12 — Observabilidade
Mesmo sistema + métricas expostas para **Prometheus** e visualizadas no **Grafana**, cada um em seu container.

## Aula 13 — Testes
Testes unitários de services, de controllers (isolando o framework web) e de persistência (isolando o framework de persistência/BD); testes de integração.

## Aula 15 — Teste de mutação
Aplicar teste de mutação sobre o exercício anterior.

## Aula 17 — CI/CD²
Slides sobre CI/CD (base para o pipeline do Pacote 2).

## Containers e Docker
"Guia de Orientação — Dockerfile" disponível em Materiais de Apoio.
