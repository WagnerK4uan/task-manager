# Tasks: 010 — Acabamento do quadro

> feature: 010-board-polish

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula).
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  A 008 terminou em T-061, então esta feature começa em T-062.

  Antes do audit --ci: verify da 010 e da 008, porque a T-065 mexe no teste
  do AC-056.
-->

## T-062 — Favicon do projeto [concluida]
- Refs: US-022, AC-059
- Arquivos: frontend/public/favicon.svg, frontend/public/favicon.ico, frontend/src/index.html
- Notas: o quadro em miniatura — fundo na cor da tinta e três colunas de alturas diferentes em âmbar, índigo e verde, as cores das situações. O `favicon.ico` padrão do Angular é substituído por um gerado do SVG, em 16, 32 e 48 px, para quem pede `/favicon.ico` sem ler o documento.

## T-063 — Confirmação de exclusão na própria interface [concluida]
- Refs: US-021, AC-058, AC-056
- Arquivos: frontend/src/app/shared/confirm-dialog/confirm-dialog.ts, frontend/src/app/shared/confirm-dialog/confirm-dialog.html, frontend/src/app/features/tasks/components/task-panel/task-panel.ts, frontend/src/app/features/tasks/components/task-panel/task-panel.html, frontend/src/app/features/tasks/pages/task-board/task-board.ts
- Notas: `role="alertdialog"`, foco inicial em "Cancelar" e foco preso com `cdkTrapFocus` (RNF-46). O painel guarda se a confirmação está aberta e decide o que o Esc fecha (ASM-041). O quadro perde o `window.confirm` e só executa a exclusão que o painel emitir.

## T-064 — Painel desliza ao abrir [concluida]
- Refs: US-023, AC-060
- Arquivos: frontend/src/styles.css, frontend/src/app/features/tasks/components/task-panel/task-panel.html
- Notas: `painel-entra` desliza da direita e `veu-entra` esmaece o fundo, ambos desligados com `prefers-reduced-motion` (RNF-45).

## T-065 — Prova E2E do acabamento [concluida]
- Refs: US-021, US-022, US-023, AC-058, AC-059, AC-060, AC-056
- Arquivos: e2e/tests/kanban-polish.spec.ts, e2e/tests/kanban.spec.ts
- Notas: um teste por critério. O AC-058 falha se o navegador disparar qualquer `dialog`. O AC-060 compara o `animation-name` computado com e sem `reducedMotion: 'reduce'`. O teste do AC-056 troca o `dialog` nativo pelos botões da confirmação (ASM-043).
