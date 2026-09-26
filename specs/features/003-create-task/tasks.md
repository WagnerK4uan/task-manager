# Tasks: 003 — Criação e consulta de tarefa

> feature: 003-create-task

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 001 usou
  T-001 a T-010 e a 002 usou T-011 a T-016, então esta feature começa em T-017.

  A ordem é de dentro para fora, como na 002: contrato de dados, exceção,
  service, controller, handler. A prova vem em T-022 — antes dela, nenhum
  critério sai do papel.
-->

## T-017 — Contrato de dados da API: DTOs e mapper [pendente]

- Refs: US-006, AC-014, AC-015
- Arquivos: backend/src/main/java/com/taskmanager/application/dto/TaskCreateRequest.java, backend/src/main/java/com/taskmanager/application/dto/TaskResponse.java, backend/src/main/java/com/taskmanager/application/mapper/TaskMapper.java
- Notas: dois `record`, um por direção. `TaskCreateRequest` carrega a validação declarativa da tabela de "Regras de negócio" (`@NotBlank`, `@Size`, `@NotNull`, `@FutureOrPresent`) — nenhuma verificação manual de campo em lugar nenhum. `TaskResponse` tem os oito campos e é o mesmo corpo nas duas rotas. `TaskMapper` é uma classe com dois métodos curtos (request para entidade, entidade para response), sem Lombok e sem MapStruct (D-7); é ele que garante que a entidade JPA nunca cruza a fronteira da API (P-005).

## T-018 — Exceção de domínio para tarefa inexistente [pendente]

- Refs: US-007, AC-018
- Arquivos: backend/src/main/java/com/taskmanager/domain/exception/TaskNotFoundException.java
- Notas: cumpre a ASM-008 da 002 — a exceção nasce agora porque agora existe quem a lança. `RuntimeException` com o id na mensagem, sem importar Spring nem HTTP: ela não sabe que virará 404. Quem sabe disso é o handler da T-021.

## T-019 — Service de criação e consulta [pendente]

- Refs: AC-014, AC-017, AC-018
- Arquivos: backend/src/main/java/com/taskmanager/application/service/TaskService.java
- Notas: `@Service` com dois métodos — criar, que mapeia o request, grava pela porta e devolve o `TaskResponse`; buscar por id, que traduz o `Optional.empty()` da porta (AC-013 da 002) em `TaskNotFoundException`. A dependência declarada é a interface `TaskRepository` do domínio, nunca o adaptador nem o `TaskJpaRepository`. Sem `try/catch` (RNF-12) e sem revalidar o que o `@Valid` já recusou. Depende de T-017 e T-018.

## T-020 — Controller das duas rotas e ativação de P-004 e P-005 [pendente]

- Refs: US-006, US-007, AC-014, AC-017
- Arquivos: backend/src/main/java/com/taskmanager/presentation/controller/TaskController.java, specs/constituicao.md
- Notas: `@RestController` em `/api/tasks` com `POST` (201 e header `Location` montado a partir do id criado) e `GET /{id}` (200). `@Valid` no corpo da criação é o que liga a validação do DTO. Nenhuma lógica de negócio, nenhum acesso a repositório, nenhum `try/catch`. Esta tarefa ativa P-004 e P-005 na constituição — as duas seções entram e saem da fila no mesmo commit que cria `presentation/`, que é quando o glob delas passa a casar arquivo. Depende de T-019.

## T-021 — Contrato único de erro no handler [pendente]

- Refs: AC-015, AC-016, AC-018
- Arquivos: backend/src/main/java/com/taskmanager/presentation/handler/GlobalExceptionHandler.java, backend/src/main/java/com/taskmanager/presentation/handler/ErrorResponse.java
- Notas: `@RestControllerAdvice` com os quatro mapeamentos da tabela do CLAUDE.md — `TaskNotFoundException` para 404 `TASK_NOT_FOUND`, `MethodArgumentNotValidException` para 400 `VALIDATION_ERROR` com `fields`, `HttpMessageNotReadableException` para 400 `MALFORMED_REQUEST` (é onde cai o valor de enum inexistente) e qualquer outra para 500 `INTERNAL_ERROR` sem vazar stack trace. `ErrorResponse` é um `record` em `presentation/handler` (ASM-014), com o par `field`/`message` aninhado. O `timestamp` é `Instant` em UTC (RNF-15). Se a Q-005 for respondida pelo 400, o mapeamento do identificador não numérico entra aqui também.

## T-022 — Prova executável das duas rotas [pendente]

- Refs: AC-014, AC-015, AC-016, AC-017, AC-018, AC-019
- Arquivos: backend/src/test/java/com/taskmanager/application/TaskServiceTest.java, backend/src/test/java/com/taskmanager/presentation/TaskControllerTest.java, backend/src/test/java/com/taskmanager/presentation/TaskApiIntegrationTest.java
- Notas: três fatias, cada uma no seu nível. `TaskServiceTest` é unitário com um duplo em memória da porta `TaskRepository` — sem contexto Spring, sem framework de mock. `TaskControllerTest` é `@WebMvcTest` com o service substituído por `@MockitoBean` (o `@MockBean` saiu no Spring Boot 4) e cobre AC-014 a AC-018: status, header `Location`, corpo e o contrato de erro campo a campo. `TaskApiIntegrationTest` estende o `PostgresIntegrationTest` e cobre só a AC-019 — cria, segue o `Location` e confere o id, que é a única afirmação que exige as duas rotas e o banco real ao mesmo tempo. Um `@DisplayName` com `@spec:AC-xxx` por critério, senão o gate prova zero.

## T-023 — README com as duas rotas publicadas [pendente]

- Refs: US-006, US-007
- Arquivos: README.md
- Notas: o aviso "os endpoints de tarefa chegam nas features seguintes — hoje `/api/tasks` ainda responde 404" fica falso quando o controller sobe. Ajustar a frase para dizer o que já responde e o que falta, e conferir que o exemplo de `curl` da criação casa com o payload real (ele já manda situação e prioridade, como a ASM-012 fixou). Tarefa de documentação, separada porque altera arquivo que não é código da feature.
