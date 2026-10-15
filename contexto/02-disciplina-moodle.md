# Disciplina no Moodle — 46504-04 Construção de Software (Turma 330, 2026/2)

- **Professor:** Michael da Costa Mora — michael.mora@pucrs.br
- **Moodle:** https://moodle.pucrs.br/course/view.php?id=96191
- **Aulas:** segunda e quarta, 21:00–22:30 (2NP / 4NP), Sala 406, Prédio 32
- **Comunicação:** Mural de notícias + fóruns ("Comunicações do professor", "Fórum de Dúvidas" — vazio até 14/10). O professor responde no fórum até a aula seguinte.

## Avaliação
Disciplina de **avaliação especial**: `G1 = (T1 + T2 + T3) / 3`, todas avaliações práticas. Média para aprovação G1 = 5,0; **não há G2**.

## Trabalho Prático (card "Trabalho Discente Efetivo — TDE")
- Enunciado: `01-enunciado-projeto-final.md`
- **Cronograma de entregas:**
  | Pacote | Data |
  |---|---|
  | **Pacote 1 – Processo de Desenvolvimento e Requisitos** | **14/10/2026 até 23:59** (sala aberta desde 05/10) |
  | Pacote 2 – Arquitetura e Infraestrutura | 04/11/2026 |
  | Pacote 3 – Checkpoint do Desenvolvimento | 23/11/2026 |
  | Pacote 4 – Produto Completo | 02/12/2026 |
- **Sala de Entrega – Pacote 1:** https://moodle.pucrs.br/mod/assign/view.php?id=3923522 — sem instruções extras de formato; status em 14/10 18:40: nenhum envio feito.
- **Grupo do Mateus: Grupo 9 (3/5)** — Gabriel Gerhardt de Marque, Mateus Granja de Andrade Madalosso Tieppo, Filipe Tesche Roman. (Escolha de grupos fecha 16/10 23:59.)
- Último aviso do professor (07/10): aula usada para avançar com o trabalho, sem chamada.

## Ementa e objetivos (resumo)
Fundamentos de construção de software; reutilização (engenharia de domínio, linhas de produto, frameworks, componentes); arquiteturas dirigidas por modelos (MDSE: PIM, PSM, DSLs, transformações).
Unidades: (1) Fundamentos — complexidade, mudança, construção e verificação, padrões; (2) Reutilização de software; (3) Arquiteturas orientadas a modelos.

## Linha do tempo das aulas (o que a turma já viu)
| Aula | Data | Tema |
|---|---|---|
| 01 | 03/08 | Apresentação da disciplina; exercício preparatório (catálogo de tópicos conhecidos) |
| 02 | 05/08 | Princípios para um bom código — Clean Code, Clean Architecture, SOLID (apresentações em grupo) |
| 03 | 10/08 | Review de Arquitetura de Software (estilos, padrões, microsserviços, BFF, microfrontends, Gateway, Circuit Breaker, Saga) |
| 04–05 | 12–17/08 | Containers/Docker + exercício: API de estudantes/disciplinas/matrícula em microsserviços (BD centralizado) |
| 06–07 | 19–24/08 | Padrões de microsserviços (apresentações: API Gateway/API Composition, Service Discovery, Saga + DB per Service, Circuit Breaker + Bulkhead, CQRS) |
| 08, 10 | 26/08, 02/09 | Spring Cloud: guias spring.io (Service Discovery, Config centralizada, Gateway); exercício peças/clientes/representantes em 3 microsserviços |
| 09 | 31/08 | **Engenharia de Software com IA — SDD (Spec-Driven Development)**; codelab com Antigravity (AGY CLI). Ver `03-sdd-ia-aula09.md` |
| 11–12 | 09–14/09 | Observabilidade: OTel, Prometheus, Grafana (containers dedicados) |
| 13–14 | 16–21/09 | Testes unitários (services, controllers isolando o web framework, persistência isolando o BD) e de integração |
| 15–16 | 23–28/09 | Teste de mutação; finalização dos exercícios |
| 17 | 01/10 | CI/CD² |
| 18–19 | 05–07/10 | Início / avanço do TF Integrador |

Stack que a turma vem usando nos exercícios: **Java + Spring Boot + Spring Cloud** (Gateway, Eureka/Service Discovery, Config Server), Docker/Docker Compose, Prometheus + Grafana, JUnit/Mockito, testes de mutação, CI/CD. Detalhes em `04-exercicios-anteriores.md`.
