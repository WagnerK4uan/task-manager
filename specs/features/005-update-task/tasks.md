# Tasks: 005 — Substituição e troca de situação da tarefa

> feature: 005-update-task

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 004 terminou
  em T-028, então esta feature começa em T-029.

  A ordem continua de dentro para fora — DTO, tradução, aplicação, borda — e a
  prova vem em T-033. A Q-007 precisa estar respondida antes da T-029: é ela que
  decide uma anotação do DTO e o sentido da AC-029.
-->

## T-029 — DTOs de substituição e de troca de situação [concluida]

- Refs: US-010, US-011, AC-028, AC-029, AC-032
- Arquivos: backend/src/main/java/com/taskmanager/application/dto/TaskUpdateRequest.java, backend/src/main/java/com/taskmanager/application/dto/TaskStatusUpdateRequest.java
- Notas: dois `record` em `application/dto`. O `TaskUpdateRequest` repete as validações do `TaskCreateRequest` para título, descrição, situação e prioridade, com as mesmas mensagens — e o prazo segue o que a Q-007 decidir. O `TaskStatusUpdateRequest` tem um campo só, `@NotNull`. São DTOs próprios, não reaproveitamento do de criação, pela razão registrada em "Impacto técnico" da spec.

## T-030 — Cópia dos campos da substituição no mapper [concluida]

- Refs: US-010, AC-027
- Arquivos: backend/src/main/java/com/taskmanager/application/mapper/TaskMapper.java
- Notas: um `aplicar(Task, TaskUpdateRequest)` `void`, que escreve os cinco campos na entidade recebida pelos setters que a T-012 já criou. Não instancia `Task` nova: id e `createdAt` da tarefa gravada têm que sobreviver à edição. A troca de situação da T-031 não passa por aqui — é um setter só, e um método de mapper para ele seria embrulho.

## T-031 — Substituição e troca de situação no service [concluida]

- Refs: US-010, US-011, AC-027, AC-030, AC-031, AC-033
- Arquivos: backend/src/main/java/com/taskmanager/application/service/TaskService.java
- Notas: `substituir(id, request)` e `alterarStatus(id, request)`, os dois carregando pela porta, aplicando e devolvendo `TaskMapper.paraResposta(repositorio.gravar(tarefa))`. Ausência vira `TaskNotFoundException` reaproveitando a tradução que a T-019 da 003 já fez — a decisão do 404 não se repete. Sem `@Transactional`: a consequência está registrada na ASM-019. Altera arquivo da T-024 da 004.

## T-032 — Rotas de substituição e de troca de situação no controller [concluida]

- Refs: US-010, US-011, AC-027, AC-028, AC-031, AC-032
- Arquivos: backend/src/main/java/com/taskmanager/presentation/controller/TaskController.java
- Notas: `@PutMapping("/{id}")` e `@PatchMapping("/{id}/status")`, ambos com `@Valid @RequestBody` e `@PathVariable Long id`, ambos devolvendo `TaskResponse` — 200 é o padrão, sem `ResponseEntity`. Nada de lógica e nada de `try/catch` (RNF-22). Depende de T-031.

## T-033 — Prova executável da edição e da troca de situação [concluida]

- Refs: AC-027, AC-028, AC-029, AC-030, AC-031, AC-032, AC-033
- Arquivos: backend/src/test/java/com/taskmanager/application/TaskServiceTest.java, backend/src/test/java/com/taskmanager/presentation/TaskControllerTest.java, backend/src/test/java/com/taskmanager/presentation/TaskApiIntegrationTest.java
- Notas: as três classes existentes são estendidas, cada critério no nível onde ele é observável. `TaskApiIntegrationTest` cobre a AC-027, porque `createdAt` preservado e `updatedAt` posterior só se provam contra o banco real que preenche as duas colunas. `TaskServiceTest` cobre AC-030 (404 e nenhuma tarefa criada, checando o duplo em memória) e AC-031 (situação trocada, demais campos idênticos). `TaskControllerTest` cobre AC-028 (campo a campo, service não chamado), AC-029 (prazo vencido aceito, 200), AC-032 (`null` é `VALIDATION_ERROR`, `URGENTE` é `MALFORMED_REQUEST`) e AC-033 (404 no contrato único). Um `@DisplayName` com `@spec:AC-xxx` por critério. O verify é o último passo, depois dos commits e dos saves (L-003).

## T-034 — README com as rotas de edição publicadas [concluida]

- Refs: US-010, US-011
- Arquivos: README.md
- Notas: `PUT /api/tasks/{id}` e `PATCH /api/tasks/{id}/status` saem de "próxima feature" para implementada na tabela da API, e o exemplo de `curl` do `PATCH` perde a ressalva de rota não implementada. O bloco de estado atual passa a citar a 005 e a dizer que só a exclusão continua respondendo 405. Tarefa de documentação, separada porque altera arquivo que não é código da feature.
