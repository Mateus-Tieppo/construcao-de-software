# Aula 09 (31/08) — Engenharia de Software com IA: SDD

> Resumo dos slides "A Transição para o SDD" do Prof. Michael Mora. É a base esperada para o item
> **"Descrição do processo de desenvolvimento suportado por IA"** do Pacote 1 — o professor vai
> esperar ver estes conceitos aplicados ao processo do grupo.

## Ideia central
Com LLMs gerando código em volume, o gargalo deixa de ser escrever código e passa a ser **entender, validar e orquestrar**. O **Spec-Driven Development (SDD)** torna a **especificação a fonte única de verdade**: o código é derivado dela e validado contra ela; a documentação é um contrato executável com portões de validação, não um texto passivo escrito depois.

## Por que (problemas do "vibe coding")
- Termo do Karpathy (fev/2025): pedir em linguagem vaga e aceitar o código sem revisão.
- Dados citados: aumento de ~48% em copy-paste (GitClear), maior taxa de segredos expostos em código assistido por IA, parcela relevante de código gerado com vulnerabilidades, e o estudo METR em que devs se *sentiram* mais rápidos mas foram ~19% mais lentos.
- **Triple Debt Model** (Margaret-Anne Storey): débito técnico + **cognitivo** (ninguém tem o modelo mental do código) + **de intenção**.

## Limitações de LLM a considerar
- LLM é geração probabilística (próximo token), não recuperação determinística.
- **"Lost in the middle"**: atenção em curva U — o meio de contextos longos é pouco considerado.
  Mitigações: regras inegociáveis de arquitetura no **início** e requisitos de teste no **fim** do prompt; filtrar contexto (3–5 trechos relevantes); **revelação progressiva** (quebrar em passos menores).

## Elementos do SDD citados
- 6 elementos estruturais de uma spec eficaz e 3 níveis de maturidade de SDD (o slide é imagem — usar o modelo de spec do `CLAUDE.md`: objetivo, contexto, restrições, interfaces/contratos, critérios de aceitação/verificação, fora de escopo).
- Balancear o peso: spec extensa não vale para bugfix de uma linha; é indispensável para plataforma grande com vários devs/agentes.

## Agentes
- Chatbot (stateless, uma resposta) vs **agente de codificação** (ciclo Observar → Raciocinar → Agir → Verificar → Repetir; acessa repositório, MCP, roda compilador/linter, corrige a si mesmo).
- **Padrão de Agente Adversarial**:
  - *Coordenador (Tech Lead)* — lê a spec raiz e gera sub-specs;
  - *Implementadores* — subagentes em paralelo, cada um com uma sub-spec, sem colidir arquivos;
  - *Verificador (Juiz)* — avalia estritamente contra os Critérios de Verificação, buscando violações.

## Harness Engineering
**Agente autônomo = LLM + Harness (infraestrutura ao redor).**
- **Controles feedforward (preventivos):** constituição do projeto (`AGENTS.md`, `.cursorrules`, **`CLAUDE.md`**) lida no início de cada sessão; *progressive disclosure* (documento mestre enxuto que aponta para skills); skills reutilizáveis (ex.: `.github/instructions/`).
- **Controles feedback (sensores):** computacionais — testes, linters, *fitness functions* arquiteturais (ArchUnit, Dependency Cruiser); inferenciais — LLM-as-a-judge revisando nomenclatura, cobertura, segurança.

## Papel do engenheiro
Inner loop (codar/testar) automatizado → surge o **Middle Loop / Supervisory Engineering**:
- **Direcionamento** — decompor em blocos do tamanho de um agente, especificar intenção, gerenciar contexto, codificar padrões em skills;
- **Avaliação** — revisar o que o código *faz*, caçar alucinações plausíveis;
- **Correção** — integrar saídas de agentes paralelos e reconduzir à consistência.
Outer loop = governança.

## Ferramentas comparadas
Cursor (IDE, `.cursorrules`), Windsurf (IDE, Cascade), **Claude Code** (terminal-first, subagentes paralelos, `CLAUDE.md` + hooks, indicado para SDD e auditorias via CI/CD).

## Desafios
Não-determinismo (mesma spec → código diferente ⇒ usar sensores e *property-based testing*), custo de tokens, overhead metodológico (dosar o rigor pelo risco).

## Atividade prática da aula
Codelab com Antigravity (AGY CLI) — link no Moodle (Aula 09).
