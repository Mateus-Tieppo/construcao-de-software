# ADR-006: Fluxo de entrega, CI e baselines

**Estado:** Proposta para revisão do grupo · 04/11/2026 (o fluxo de branches é decisão do Pacote 1; a configuração técnica é proposta)

## Contexto
O Pacote 1 (seção 1.7) definiu Git Flow simplificado: `main`, `develop`, `feature/*`, `fix/*`, `docs/*`, PR com CI verde e uma aprovação humana, *squash* em `develop`, tags `pacote-N`. O enunciado exige CI no GitHub Actions a cada Pull Request ou merge em `develop`. Na inspeção de 04/11/2026 só existiam `main` e branches `docs/*`; não havia `develop`, workflows nem proteção de branch verificada.

## Decisão proposta
1. Manter o fluxo do Pacote 1 sem mudanças de nome.
2. Workflow `ci.yml` com gatilhos `pull_request` (destino `develop` e `main`) e `push` em `develop` (o merge de um PR gera um `push`; não existe evento chamado `merge`).
3. Workflow `release.yml` em `push` de tag `pacote-*` ou `v*`: constrói e publica imagens no GHCR.
4. Proteção de branch em `develop` e `main`: PR obrigatório, 1 aprovação, *status checks* obrigatórios, sem *force push*.
5. Até existir código e `develop`, entregas documentais seguem `docs/*` → PR → `main` com revisão humana, e a tag `pacote-N` marca a baseline.

## Alternativas
1. GitHub Flow puro (só `main`): mais simples, mas o enunciado cita `develop`.
2. Git Flow completo com `release/*` e `hotfix/*`: excessivo para três pessoas e quatro entregas.

## Consequências
- Criar `develop`, os workflows e a proteção é tarefa do início do Pacote 3 (US32/US33), feita por um integrante com permissão de admin no repositório.
