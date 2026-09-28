# Tasks: 008 — Quadro kanban de tarefas

> feature: 008-kanban-board

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  A primeira tentativa desta feature entregou uma lista, recusada pelo
  mantenedor. Os códigos dela — AC-040 a AC-046 e T-043 a T-049 — ficam vagos
  para sempre: código de rastreio não se reaproveita. Esta feature começa em
  T-052, depois do T-051 da 009.

  A ordem é de dentro para fora: tipos e serviço, paleta e casca, card, quadro,
  arrasto, botão, painel, criação — e a prova vem na T-061.

  Todo arquivo criado sob frontend/src/app/ tem que aparecer em algum Arquivos:
  aqui. O ARQUIVO_ORFAO é aviso no audit e erro no audit --ci.
-->

## T-052 — Tipos, serviço com criar e substituir, e o princípio P-006 [concluida]
- Refs: US-017, US-019, US-020, AC-050, AC-055
- Arquivos: frontend/src/app/features/tasks/models/task.ts, frontend/src/app/core/services/task-api.ts, specs/constituicao.md
- Notas: os tipos espelham os DTOs da API, incluindo o `fields` do envelope de erro, que é o que a AC-055 mostra campo a campo. O `task-api.ts` é o **único** arquivo com `HttpClient` e ganha `criar` e `substituir` além de `listar`, `alterarStatus` e `excluir`; a falha traduzida carrega status, código e os campos recusados. A constituição entra aqui porque o P-006 tem que estar ativo no mesmo commit que cria os arquivos do glob `frontend/src/app/features/**`.

## T-053 — Paleta por situação, Tailwind e a casca [concluida]
- Refs: US-017
- Arquivos: frontend/package.json, frontend/.postcssrc.json, frontend/src/styles.css, frontend/src/app/app.config.ts, frontend/src/app/app.routes.ts, frontend/src/app/app.html
- Notas: `tailwindcss` e `@tailwindcss/postcss` em devDependencies, `.postcssrc.json` com o plugin e `@import "tailwindcss"` no `styles.css` — a v4 dispensa `tailwind.config`. Os tokens `@theme` declaram a cor de cada situação (âmbar, índigo, verde) e de cada prioridade, mais papel, tinta e fio. Junto vêm `provideHttpClient`, a rota do quadro com o redirecionamento da raiz e o cabeçalho.

## T-054 — Card da tarefa [concluida]
- Refs: US-017, AC-048
- Arquivos: frontend/src/app/features/tasks/components/task-card/task-card.ts, frontend/src/app/features/tasks/components/task-card/task-card.html
- Notas: componente de apresentação puro — recebe uma `Task` e emite o que o quadro executa. Nenhum `HttpClient` aqui, que é o que o P-006 garante por gate. Mostra título, etiqueta de prioridade colorida e prazo em `dd/mm/aaaa`, com ausência explícita quando não há prazo. A tradução dos enums para português vive nesta camada.

## T-055 — Quadro de três colunas [concluida]
- Refs: US-017, AC-048, AC-049, AC-050
- Arquivos: frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html
- Notas: estado em `signal` (ASM-036). Uma chamada de listagem sem filtro alimenta as três colunas, agrupadas por situação na apresentação; o contador de cada coluna é a quantidade de cards dela. Coluna sem card mostra convite, que não é erro (AC-049); falha da API é mensagem legível, sem status cru (AC-050). Em largura de telefone as colunas empilham.

## T-056 — Arrastar o card entre colunas [concluida]
- Refs: US-018, AC-051
- Arquivos: frontend/package.json, frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html, frontend/src/app/features/tasks/components/task-card/task-card.html
- Notas: `@angular/cdk` em dependências (ASM-037), com `cdkDropListGroup`, `cdkDropList` por coluna e `cdkDrag` no card. Soltar numa coluna diferente chama `PATCH /status` e recarrega o quadro; soltar na mesma coluna não chama nada. Falha do servidor devolve o card para a origem, com aviso — o quadro nunca mostra estado que o banco não tem (RNF-40).

## T-057 — Mover o card pelo botão [concluida]
- Refs: US-018, AC-052
- Arquivos: frontend/src/app/features/tasks/components/task-card/task-card.ts, frontend/src/app/features/tasks/components/task-card/task-card.html, frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html
- Notas: avançar e voltar de coluna por botão, com rótulo acessível que diz para onde vai, alcançável por teclado (RNF-43). É a mesma chamada do arrasto — dois caminhos, uma operação. O botão de avançar some na última coluna e o de voltar na primeira.

## T-058 — Painel do card: ler, editar e excluir [concluida]
- Refs: US-019, AC-053, AC-054, AC-055, AC-056
- Arquivos: frontend/src/app/features/tasks/components/task-panel/task-panel.ts, frontend/src/app/features/tasks/components/task-panel/task-panel.html, frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html
- Notas: painel lateral com o quadro visível atrás, aberto ao acionar o card. Formulário reativo com título, descrição, situação, prioridade e prazo; salvar é `PUT` com a tarefa inteira. A validação vem do backend e é exibida por campo, a partir do `fields` do envelope (ASM-039) — nada de regra duplicada no TypeScript. Excluir pede confirmação e fecha o painel. Fecha por `Esc` e pelo botão, devolvendo o foco ao card.

## T-059 — Criar tarefa pela coluna [concluida]
- Refs: US-020, AC-057
- Arquivos: frontend/src/app/features/tasks/components/task-panel/task-panel.ts, frontend/src/app/features/tasks/components/task-panel/task-panel.html, frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html
- Notas: cada coluna tem um botão de criar que abre o mesmo painel, em branco e já com a situação daquela coluna; salvar é `POST`. O painel sabe se tem id — é o que distingue criar de editar, e é por isso que não existem dois componentes.

## T-060 — Prova de ponta a ponta do quadro [concluida]
- Refs: AC-048, AC-049, AC-050, AC-051, AC-052, AC-053, AC-054, AC-055, AC-056, AC-057
- Arquivos: e2e/tests/kanban.spec.ts
- Notas: dez testes, um por critério, com o título começando em `@spec:AC-xxx`. Seleção por papel, texto acessível ou `data-testid`, nunca por classe (RNF-41). Cada teste cria os próprios dados pela API, com títulos únicos, e não assume banco vazio nem conta o total de cards do quadro (ASM-026 da 007) — o contador da coluna é comparado com os cards que aquela coluna mostra, nunca com um número fixo. As AC-049 e AC-050 interceptam a listagem no navegador, devolvendo lista vazia e erro. O arrasto da AC-051 é dirigido por eventos de mouse em passos (ASM-040), mirando uma âncora estável no topo da coluna de destino — o centro da coluna sai da tela conforme o quadro cresce. Cada teste apaga pela API as tarefas que criou, para o quadro não crescer sem limite entre execuções. O verify é o último passo, depois dos commits e dos saves.

## T-061 — README com o quadro publicado [concluida]
- Refs: US-017, US-018, US-019, US-020
- Arquivos: README.md
- Notas: o bloco de estado atual passa a dizer que o sistema tem quadro kanban, com o que ele faz; a tabela de stack ganha Tailwind e `@angular/cdk`. Tarefa de documentação, separada porque não altera código da feature.

<!--
  Antes do audit --ci: rodar o verify de TODAS as features, não só desta —
  um commit desta feature invalida a prova das anteriores por mtime (L-002), e
  cada verify sobe o compose e roda o E2E (ASM-025 da 007).
-->
