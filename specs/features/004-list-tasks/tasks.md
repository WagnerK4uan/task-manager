# Tasks: 004 — Listagem com filtros de título e situação

> feature: 004-list-tasks


## T-024 — Listagem no service, com filtro em branco normalizado [concluida]

- Refs: US-008, AC-021, AC-022, AC-023
- Arquivos: backend/src/main/java/com/taskmanager/application/service/TaskService.java
- Notas: um método `listar(titulo, status)` que normaliza título em branco para nulo (ASM-017) e delega para a `buscar` da porta, mapeando cada entidade com `stream().map(TaskMapper::paraResposta).toList()`. A porta não muda (RNF-16): ordem e semântica dos filtros já são dela, provadas pela AC-012 da 002. Lista vazia volta vazia — traduzir isso em erro não é papel de ninguém aqui. Altera arquivo da T-019 da 003, o que está justificado em "Impacto técnico" da spec.

## T-025 — Rota de listagem no controller [concluida]

- Refs: US-008, AC-021, AC-022, AC-024
- Arquivos: backend/src/main/java/com/taskmanager/presentation/controller/TaskController.java
- Notas: `@GetMapping` na raiz de `/api/tasks`, com `@RequestParam(required = false) String title` e `@RequestParam(required = false) TaskStatus status`. A conversão do texto para o enum é do Spring — valor fora do enum vira `MethodArgumentTypeMismatchException`, que o handler da 003 já traduz em 400 `MALFORMED_REQUEST` (AC-024), sem uma linha nova de código. Nada de `try/catch` e nada de lógica: o controller entrega os dois parâmetros e devolve a lista. Depende de T-024.

## T-026 — Erros de protocolo no contrato único de erro [concluida]

- Refs: US-009, AC-025, AC-026
- Arquivos: backend/src/main/java/com/taskmanager/presentation/handler/GlobalExceptionHandler.java, CLAUDE.md
- Notas: `@ExceptionHandler(NoResourceFoundException.class)` (`org.springframework.web.servlet.resource`) para 404 `RESOURCE_NOT_FOUND`, antes que o `Exception.class` a capture e a transforme em 500. A tabela de erros do CLAUDE.md ganha a linha correspondente no mesmo commit — a tabela e o código que a implementa mudam juntos. A Q-006 foi respondida pelo mapeamento: `HttpRequestMethodNotSupportedException` (`org.springframework.web`) entra no mesmo commit, para 405 `METHOD_NOT_ALLOWED` (AC-026), com o header `Allow` montado a partir de `getSupportedMethods()` — um 405 que não diz o que a rota aceita obriga quem integra a adivinhar. As duas linhas novas entram na tabela de erros do CLAUDE.md junto com o código.

## T-027 — Prova executável da listagem e dos erros de protocolo [concluida]

- Refs: AC-021, AC-022, AC-023, AC-024, AC-025, AC-026
- Arquivos: backend/src/test/java/com/taskmanager/application/TaskServiceTest.java, backend/src/test/java/com/taskmanager/presentation/TaskControllerTest.java, backend/src/test/java/com/taskmanager/presentation/TaskApiIntegrationTest.java
- Notas: as três classes da T-022 da 003 são estendidas, cada critério no nível onde ele é de fato observável. `TaskServiceTest` cobre AC-022 e AC-023 com o duplo em memória da porta — é onde a normalização do filtro em branco e a lista vazia se provam sem HTTP. `TaskControllerTest` cobre AC-021 (corpo e ordem do array) e AC-024 (situação fora do enum vira 400) com o service substituído. `TaskApiIntegrationTest` cobre AC-025 e AC-026 contra a aplicação de pé, porque 404 de roteamento e 405 de método só existem com o dispatcher real — provar isso com o service mockado seria provar a montagem do teste, não o comportamento. Um `@DisplayName` com `@spec:AC-xxx` por critério.

## T-028 — README com a listagem publicada [concluida]

- Refs: US-008
- Arquivos: README.md
- Notas: a linha `GET /api/tasks?title=&status=` sai de "próxima feature" para implementada, e o exemplo de `curl` da listagem deixa de vir com a ressalva de rota não implementada. O bloco de estado atual passa a citar a 004. Tarefa de documentação, separada porque altera arquivo que não é código da feature.
