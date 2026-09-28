# Tasks: 008 — Listagem de tarefas na interface

> feature: 008-frontend-tasks

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 007 terminou
  em T-042, então esta feature começa em T-043.

  Só começa depois da 007: sem o harness, nenhum critério daqui tem como ser
  provado. A ordem é de dentro para fora — tipos, serviço, casca, componente,
  página, ações — e a prova vem na T-049.

  Todo arquivo criado sob frontend/src/app/ tem que aparecer em algum Arquivos:
  aqui. O ARQUIVO_ORFAO é aviso no audit e erro no audit --ci.
-->

## T-043 — Tipos da tarefa, serviço HTTP e o princípio P-006 [pendente]

- Refs: US-014, US-015, AC-040, AC-043, AC-046
- Arquivos: frontend/src/app/features/tasks/models/task.ts, frontend/src/app/core/services/task-api.ts, specs/constituicao.md
- Notas: os tipos espelham os DTOs da API (`Task`, `TaskStatus`, `TaskPriority` e o envelope de erro com `timestamp`, `status`, `error`, `message`, `path`). O `task-api.ts` é o **único** arquivo com `HttpClient`: `listar(titulo, status)`, `alterarStatus(id, status)`, `excluir(id)`, e a tradução do envelope de erro numa função — sem interceptor (ASM-031). A constituição entra nesta tarefa porque o princípio tem que chegar no mesmo commit que cria os arquivos do glob `frontend/src/app/features/**`, e este é esse commit: o P-006 sai da fila e vira seção.

## T-044 — Tailwind e a casca da aplicação [pendente]

- Refs: US-014
- Arquivos: frontend/package.json, frontend/.postcssrc.json, frontend/src/styles.css, frontend/src/app/app.config.ts, frontend/src/app/app.routes.ts, frontend/src/app/app.html
- Notas: `tailwindcss` e `@tailwindcss/postcss` em devDependencies, um `.postcssrc.json` com o plugin e `@import "tailwindcss";` no `styles.css` — a v4 dispensa `tailwind.config`, e o `@angular/build` já procura config de PostCSS (ASM-029). Junto vêm o `provideHttpClient` no `app.config.ts`, a rota da listagem com o redirecionamento da raiz no `app.routes.ts` e o cabeçalho no `app.html`. É um commit só porque o Tailwind sem a casca não se vê e a casca sem o Tailwind teria estilo para jogar fora. Altera os arquivos que a T-009 da 001 criou.

## T-045 — Linha da tarefa [pendente]

- Refs: US-014, AC-040
- Arquivos: frontend/src/app/features/tasks/components/task-row/task-row.ts, frontend/src/app/features/tasks/components/task-row/task-row.html
- Notas: componente de apresentação puro: recebe uma `Task` e emite as ações de concluir e excluir. Nenhum `HttpClient` aqui — é o que o P-006 passa a garantir por gate. A tradução dos enums para português e a formatação do prazo como `dd/mm/aaaa` vivem nesta camada, num mapa simples. Sem arquivo de estilo: os utilitários vão no template, e estilo que não couber em utilitário vai nomeado no `styles.css`.

## T-046 — Página de listagem com filtros na query string [pendente]

- Refs: US-014, AC-040, AC-041, AC-042, AC-043
- Arquivos: frontend/src/app/features/tasks/pages/task-list/task-list.ts, frontend/src/app/features/tasks/pages/task-list/task-list.html
- Notas: estado em `signal` (ASM-027), sem biblioteca de estado. Os dois filtros são lidos e escritos na query string da rota, para que recarregar a página preserve o filtro (ASM-028, AC-041). Lista vazia é estado vazio e não erro (AC-042); falha da API é mensagem legível, sem status cru (AC-043). Os controles levam rótulo e `data-testid`, porque a T-049 não pode selecionar por classe (RNF-35). Depende de T-043, T-044 e T-045.

## T-047 — Concluir e excluir a partir da lista [pendente]

- Refs: US-015, AC-044, AC-045, AC-046
- Arquivos: frontend/src/app/features/tasks/pages/task-list/task-list.ts, frontend/src/app/features/tasks/pages/task-list/task-list.html, frontend/src/app/features/tasks/components/task-row/task-row.html
- Notas: as duas ações chamam o serviço e **recarregam a lista** da API em vez de remendar o array local (ASM-030). A exclusão pede confirmação antes de chamar a API (AC-045) — é a interface que pergunta, como a 006 fixou. O 404 da exclusão é tratado como "já não está lá", sem mensagem de erro (AC-046, contrapartida da Q-008 da 006). Altera arquivos da T-045 e da T-046.

## T-048 — Prova E2E da listagem [pendente]

- Refs: AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046
- Arquivos: e2e/tests/task-list.spec.ts
- Notas: sete testes, um por critério, com o título começando em `@spec:AC-xxx` — é o título que o `spec-tap.mjs` lê como nome do caso. Seleção por papel, texto acessível ou `data-testid`, nunca por classe (RNF-35). Cada teste cria os próprios dados pela API, com títulos únicos, e não assume banco vazio nem conta o total de linhas (ASM-026 da 007). A AC-043 é provada interceptando a rota no navegador para devolver 500 (Q-011), sem derrubar o backend. A AC-046 exclui a tarefa pela API antes de acionar a exclusão na tela. A AC-045 exercita a confirmação recusada e a aceita, no mesmo teste, porque o critério tem os dois lados. O verify é o último passo, depois dos commits e dos saves (L-003).

## T-049 — README com a tela publicada [pendente]

- Refs: US-014, US-015
- Arquivos: README.md
- Notas: o bloco de estado atual passa a dizer que o sistema tem interface, com a listagem descrita e o que ela faz; a tabela de stack ganha Tailwind. A exceção ao D-3 e o `e2e/` já foram documentados pela T-042 da 007 — esta tarefa é só a tela. Tarefa de documentação, separada porque não altera código da feature.

<!--
  Antes do audit --ci: rodar o verify de TODAS as features, não só desta —
  um commit desta feature invalida a prova das anteriores por mtime (L-002), e
  cada verify sobe o compose e roda o E2E (ASM-025 da 007).
-->
