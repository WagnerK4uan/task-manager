# Tasks: 011 — Exclusão em lote

> feature: 011-batch-delete

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula).
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  A 010 terminou em T-065, então esta feature começa em T-066.
-->

## T-066 — Porta e adaptador de exclusão em lote [concluida]
- Refs: US-024, AC-061, AC-062
- Arquivos: backend/src/main/java/com/taskmanager/domain/repository/TaskRepository.java, backend/src/main/java/com/taskmanager/infrastructure/persistence/TaskJpaRepository.java, backend/src/main/java/com/taskmanager/infrastructure/persistence/TaskRepositoryAdapter.java, backend/src/test/java/com/taskmanager/infrastructure/TaskRepositoryPortTest.java, backend/src/test/java/com/taskmanager/application/TaskServiceTest.java
- Notas: a porta ganha `contarExistentes` e `excluirPorIds`. O adaptador conta com a consulta derivada `countByIdIn` e exclui com `deleteAllByIdInBatch`, um comando só (RNF-49). O duplo em memória do `TaskServiceTest` implementa os dois.

## T-067 — Endpoint de exclusão em lote [concluida]
- Refs: US-024, AC-061, AC-062, AC-063, AC-064
- Arquivos: backend/src/main/java/com/taskmanager/application/dto/TaskBatchDeleteRequest.java, backend/src/main/java/com/taskmanager/application/service/TaskService.java, backend/src/main/java/com/taskmanager/presentation/controller/TaskController.java, backend/src/test/java/com/taskmanager/application/TaskServiceTest.java, backend/src/test/java/com/taskmanager/presentation/TaskControllerTest.java, backend/src/test/java/com/taskmanager/presentation/TaskApiIntegrationTest.java, CLAUDE.md, README.md
- Notas: `record TaskBatchDeleteRequest(@NotEmpty List<@NotNull Long> ids)`. O service tira as repetições, confere se todos existem e só então exclui (ASM-045). O `GlobalExceptionHandler` já traduz 404 e 400.

## T-068 — Modo seleção no quadro [concluida]
- Refs: US-025, AC-065, AC-066, AC-067
- Arquivos: frontend/src/app/core/services/task-api.ts, frontend/src/app/features/tasks/pages/task-board/task-board.ts, frontend/src/app/features/tasks/pages/task-board/task-board.html, frontend/src/app/features/tasks/components/task-card/task-card.ts, frontend/src/app/features/tasks/components/task-card/task-card.html
- Notas: o quadro guarda o modo, os ids marcados e se a confirmação do lote está aberta; o Esc fecha só a confirmação. No modo, o arrastar desliga e as setas e o "+ Nova tarefa" somem (ASM-047). A confirmação reaproveita o `ConfirmDialog` (RNF-50).

## T-069 — Prova E2E da exclusão em lote [concluida]
- Refs: US-025, AC-065, AC-066, AC-067
- Arquivos: e2e/tests/batch-delete.spec.ts
- Notas: um teste por critério, no molde do `kanban-polish.spec.ts`. O AC-066 falha se o navegador disparar qualquer `dialog`.

## T-070 — Barra de seleção cabe em tela estreita [pendente]
- Refs: US-025, AC-065
- Arquivos: frontend/src/app/features/tasks/pages/task-board/task-board.html
- Notas: em 357 px a contagem e o "Excluir selecionadas" quebravam em duas linhas. Abaixo de `sm` a barra ocupa a linha inteira, nenhum texto quebra e o botão mostra só "Excluir", mantendo o nome acessível "Excluir selecionadas".
