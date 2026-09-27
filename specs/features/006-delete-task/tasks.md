# Tasks: 006 — Exclusão da tarefa

> feature: 006-delete-task

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 005 terminou
  em T-034, então esta feature começa em T-035.

  A ordem é a de sempre, de dentro para fora — aplicação, borda, prova,
  documentação —, e é curta porque não há classe nova: a porta já sabe excluir
  desde a 002. A Q-008 precisa estar respondida antes da T-035: é ela que decide
  se o service carrega antes de excluir e o sentido da AC-036.
-->

## T-035 — Exclusão no service [concluida]

- Refs: US-012, AC-034, AC-035, AC-036
- Arquivos: backend/src/main/java/com/taskmanager/application/service/TaskService.java
- Notas: um `excluir(Long id)` `void`, que chama o `carregar(id)` privado — a mesma tradução de ausência em `TaskNotFoundException` que a T-019 da 003 criou, agora no quarto chamador — e em seguida `repositorio.excluirPorId(id)`. Não devolve a tarefa excluída (RNF-27). Sem `@Transactional`: a consequência está na ASM-023. A porta não muda (RNF-24) e não ganha `existe(id)` (ASM-022). Altera arquivo da T-024 da 004.

## T-036 — Rota de exclusão no controller [concluida]

- Refs: US-012, AC-034, AC-037
- Arquivos: backend/src/main/java/com/taskmanager/presentation/controller/TaskController.java
- Notas: `@DeleteMapping("/{id}")` com `@PathVariable Long id`, método `void` e `@ResponseStatus(HttpStatus.NO_CONTENT)` — sem `ResponseEntity`, pela razão registrada em "Impacto técnico". Nada de lógica e nada de `try/catch` (RNF-25). Só a rota do item ganha o método: a coleção continua sem `DELETE`, o que mantém a AC-026 da 004 verdadeira e é metade da AC-037. Depende de T-035.

## T-037 — Prova executável da exclusão [concluida]

- Refs: AC-034, AC-035, AC-036, AC-037
- Arquivos: backend/src/test/java/com/taskmanager/application/TaskServiceTest.java, backend/src/test/java/com/taskmanager/presentation/TaskControllerTest.java, backend/src/test/java/com/taskmanager/presentation/TaskApiIntegrationTest.java
- Notas: as três classes existentes são estendidas, cada critério no nível onde ele é observável. `TaskApiIntegrationTest` cobre a AC-034 (204 sem corpo, consulta seguinte 404, a outra tarefa intacta na listagem), a AC-036 (duas exclusões seguidas: 204 e depois 404, contra o banco real) e a AC-037 (o header `Allow` só se observa na aplicação de pé, com o dispatcher decidindo — é onde a AC-026 da 004 já mora). `TaskControllerTest` cobre a AC-034 no que é dela — 204 com corpo vazio e a delegação ao service, que é o que o `@ResponseStatus` decide — e a AC-035 com o service mockado lançando `TaskNotFoundException`, verificando os cinco campos do corpo de erro. `TaskServiceTest` cobre AC-034 e AC-035 no nível do duplo em memória: a tarefa sai do mapa, e id ausente lança sem que nada seja removido. Um `@DisplayName` com `@spec:AC-xxx` por critério. O verify é o último passo, depois dos commits e dos saves (L-003).

## T-038 — README e a ressalva envelhecida da 004 [concluida]

- Refs: US-012
- Arquivos: README.md, specs/features/004-list-tasks/spec.md
- Notas: na tabela da API a exclusão sai de "próxima feature" para implementada, e o bloco de estado atual passa a citar a 006 e para de dizer que só a exclusão falta — o backend fecha o CRUD. Um exemplo de `curl` do `DELETE` entra junto, mostrando o 204 sem corpo. No mesmo commit, a ressalva "enquanto a exclusão não existe" do Dado da AC-026 da 004 é corrigida conforme a Q-009: o critério continua o mesmo e nenhum teste muda. Tarefa de documentação, separada porque não altera código da feature.

<!--
  Antes do audit --ci: rodar o verify de TODAS as features já auditadas, não só
  desta. A prova é comparada com a data do código mais recente, e um commit
  desta feature invalida a das anteriores (L-002).
-->
