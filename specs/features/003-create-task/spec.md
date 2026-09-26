# Spec: 003 — Criação e consulta de tarefa

> feature: 003-create-task
> status: rascunho

<!--
  US-xxx = história de usuário · AC-xxx = critério de aceite
  ASM-xxx = suposição · Q-xxx = pergunta em aberto
  Todo critério de aceite vira um teste cujo título carrega @spec:AC-xxx.
-->

## Objetivo

Abrir a API: `POST /api/tasks` grava uma tarefa e responde 201 com o header
`Location`; `GET /api/tasks/{id}` devolve a tarefa gravada ou 404. É a primeira
vez que as camadas `application` e `presentation` existem — DTOs em `record`,
mapper, service, exceção de domínio e um único lugar que traduz exceção em
resposta HTTP.

## Contexto

A 002 entregou a entidade e a porta de persistência, mas nada no sistema fala
HTTP: o README ainda avisa que `/api/tasks` responde 404. As quatro operações da
porta existem e estão provadas contra PostgreSQL real; o que falta é alguém
chamá-las.

**Por que a consulta por id entra junto com a criação, numa feature chamada
`003-create-task`.** A ASM-008 da 002 fixou que a `TaskNotFoundException` nasce
com quem a lança e com o handler que a traduz. Num POST puro ninguém a lança: a
classe seria código morto e o audit a acusaria como órfã. Com o GET por id na
mesma fatia, a exceção nasce com dono e o `Location` do 201 deixa de ser um
header que ninguém segue — o teste cria, segue o header e recebe 200. O nome do
diretório fica como está, porque a constituição e a 002 já o referenciam
(ASM-013).

É também aqui que P-004 e P-005 saem da fila da constituição: o glob dos dois
aponta para `presentation/`, que passa a existir nesta feature. Pela regra de
entrada da constituição, o princípio é ativado no mesmo commit que cria o código
que ele guarda.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-006 — Registrar uma tarefa nova

Como pessoa que usa o sistema, quero registrar uma tarefa informando título,
descrição, situação, prioridade e prazo, para que ela passe a existir e eu
consiga voltar a ela depois.

#### AC-014 — A tarefa criada volta no corpo e no header Location

- **Dado** um corpo válido com título, descrição, situação, prioridade e prazo
- **Quando** a criação é enviada (`POST /api/tasks`)
- **Então** a resposta é 201, o header `Location` aponta para
  `/api/tasks/{id}` do recurso criado, e o corpo traz o `id` gerado, os cinco
  campos enviados e as duas datas de auditoria preenchidas — nunca a entidade
  de persistência

#### AC-015 — Cada campo recusado aparece com o motivo

- **Dado** um corpo sem título, ou com situação nula, ou com prioridade nula, ou
  com título de 121 caracteres, ou com prazo no passado
- **Quando** a criação é enviada
- **Então** a resposta é 400 com `error` igual a `VALIDATION_ERROR` e `fields`
  traz um par `field`/`message` por campo recusado, e nenhuma tarefa é gravada

#### AC-016 — Corpo ilegível é recusado sem vazar detalhe interno

- **Dado** um corpo com JSON sintaticamente quebrado, ou com situação
  `"URGENTE"` — valor que o enum não tem
- **Quando** a criação é enviada
- **Então** a resposta é 400 com `error` igual a `MALFORMED_REQUEST`, sem
  `fields` e sem nenhum trecho de stack trace no corpo

### US-007 — Consultar uma tarefa pelo identificador

Como pessoa que usa o sistema, quero abrir uma tarefa pelo identificador, para
que eu veja o estado atual dela — e receba um aviso claro quando ela não
existir.

#### AC-017 — A consulta devolve a tarefa gravada

- **Dado** uma tarefa já gravada
- **Quando** a consulta por identificador é feita (`GET /api/tasks/{id}`)
- **Então** a resposta é 200 e o corpo traz os oito campos da tarefa, no mesmo
  formato do corpo devolvido na criação

#### AC-018 — Tarefa inexistente responde 404 no contrato único de erro

- **Dado** um banco sem a tarefa de id `999`
- **Quando** a consulta por esse identificador é feita
- **Então** a resposta é 404 com `error` igual a `TASK_NOT_FOUND`, `status`
  `404`, `timestamp` em UTC, `message` e `path` igual a `/api/tasks/999` — e
  nenhum stack trace

#### AC-019 — O Location do 201 é navegável de ponta a ponta

- **Dado** uma criação que respondeu 201 contra o PostgreSQL real
- **Quando** o endereço do header `Location` é requisitado
- **Então** a resposta é 200 e o `id` do corpo é o mesmo do recurso criado — a
  criação e a consulta concordam sobre onde a tarefa mora

#### AC-020 — Identificador não numérico é erro de quem chama, não do servidor

- **Dado** uma consulta cujo identificador não é um número (`/api/tasks/abc`)
- **Quando** a consulta é feita
- **Então** a resposta é 400 com `error` igual a `MALFORMED_REQUEST` — nunca
  500, porque o servidor não falhou: a requisição é que está malformada

## Requisitos não funcionais

- **RNF-11** — A apresentação não conhece entidade nem repositório: P-004 e
  P-005 entram na constituição nesta feature, e a regra ArchUnit da AC-006 da
  001 continua guardando a mesma fronteira por outro caminho.
- **RNF-12** — Nenhum `try/catch` de tradução HTTP em controller ou service: o
  `@RestControllerAdvice` é o único lugar que transforma exceção em status.
- **RNF-13** — Nenhuma dependência nova. `spring-boot-starter-validation` está
  no `pom.xml` desde a 001 e só agora é usado.
- **RNF-14** — As duas rotas aparecem no `/v3/api-docs` sem anotação de
  documentação nova — o springdoc lê os tipos —, mantendo a AC-004 da 001 válida.
- **RNF-15** — O `timestamp` do erro é `Instant` em UTC, a mesma escolha das
  datas de auditoria (D-5 do CLAUDE.md).

## Regras de negócio

Corpo de entrada da criação (`TaskCreateRequest`):

| Campo | Obrigatório | Validação declarativa |
|---|---|---|
| `title` | sim | `@NotBlank`, `@Size(max = 120)` |
| `description` | não | `@Size(max = 2000)` |
| `status` | sim | `@NotNull` |
| `priority` | sim | `@NotNull` |
| `dueDate` | não | `@FutureOrPresent` |

Situação e prioridade são obrigatórias na criação (ASM-012): é o que o exemplo
do README já publica, o que as colunas `NOT NULL` exigem e o que o formulário
do frontend vai enviar.

Corpo de resposta (`TaskResponse`), igual na criação e na consulta: `id`,
`title`, `description`, `status`, `priority`, `dueDate`, `createdAt`,
`updatedAt`.

- `id`, `createdAt` e `updatedAt` são do servidor. Se vierem no corpo da
  criação, não têm efeito (Q-004).
- Qualquer situação é aceita na criação: não existe máquina de estados
  (ASM-002 da 001).
- Quem decide o 404 é a camada de aplicação: a porta devolve vazio
  (AC-013 da 002) e o service lança `TaskNotFoundException`.
- O service não revalida o que o Bean Validation já recusou — a validação de
  entrada acontece uma vez, na borda, ativada por `@Valid` no controller.

## Casos de erro

| Situação | Status | `error` |
|---|---|---|
| Campo obrigatório ausente, tamanho excedido ou prazo no passado | 400 | `VALIDATION_ERROR`, com `fields` |
| JSON sintaticamente quebrado, ou valor que o enum não tem | 400 | `MALFORMED_REQUEST` |
| Identificador inexistente na consulta | 404 | `TASK_NOT_FOUND` |
| Identificador não numérico na rota (`/api/tasks/abc`) | 400 | `MALFORMED_REQUEST` (Q-005, provado pela AC-020) |
| Qualquer outra | 500 | `INTERNAL_ERROR`, sem vazar stack trace |

## Impacto técnico

Classes criadas, todas no pacote base `com.taskmanager`:

| Classe | Pacote | Papel |
|---|---|---|
| `TaskCreateRequest` | `application.dto` | `record` de entrada da criação, com Bean Validation |
| `TaskResponse` | `application.dto` | `record` de saída das duas rotas |
| `TaskMapper` | `application.mapper` | classe com métodos simples, sem MapStruct (D-7) |
| `TaskService` | `application.service` | criar e buscar por id; lança `TaskNotFoundException` |
| `TaskNotFoundException` | `domain.exception` | a ASM-008 da 002 se cumpre: nasce com quem a lança |
| `TaskController` | `presentation.controller` | as duas rotas, com `@Valid` no corpo |
| `GlobalExceptionHandler`, `ErrorResponse` | `presentation.handler` | o único tradutor de exceção em resposta |

Decisões:

- **O payload de erro vive em `presentation/handler`, não em
  `application/dto`** (ASM-014): ele é forma de HTTP, não corpo de uma operação
  de aplicação. O service não sabe que existe um campo `status` numérico.
- **A validação é declarativa e fica no DTO.** Nenhuma verificação manual de
  campo no controller nem no service — o que o `@Valid` recusa nunca chega ao
  domínio.
- **P-004 e P-005 entram na constituição** no mesmo commit que cria
  `presentation/` (T-020), e saem da fila no fim do arquivo.
- **O README deixa de dizer que `/api/tasks` responde 404** (T-023): a frase
  vira falsa no instante em que o controller sobe.
- **A tabela de erros do CLAUDE.md ganha a linha do identificador não
  numérico** (T-021): `MethodArgumentTypeMismatchException` para 400
  `MALFORMED_REQUEST`, decidido na Q-005. Sem essa linha, o caso cairia em
  "qualquer outra" e viraria 500 por culpa do chamador.
- Nenhuma migration, nenhuma mudança de schema, nenhuma dependência nova.

## Dependências

- **Depende de:** 002-task-domain (entidade, enums e a porta) e 001-project-setup
  (validation e springdoc no `pom.xml`, base de teste com Testcontainers).
- **Bloqueia:** 004 (listagem com filtros) e o restante do CRUD; a 008
  (frontend) consome estas duas rotas.
- **Externas:** Docker rodando, para o teste de integração da AC-019.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- Listagem com filtros de título e situação — é a 004.
- Substituição (`PUT`), troca rápida de situação (`PATCH /status`) e exclusão
  (`DELETE`) — features seguintes.
- Paginação e ordenação configurável (D-8 do CLAUDE.md).
- Autenticação e escopo por usuário (ASM-003 da 001).
- Qualquer coisa de frontend (008).
- Internacionalização: as mensagens saem em uma língua só (ASM-015).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-012 | Situação e prioridade são obrigatórias na criação, sem valor padrão no DTO: quem cria diz em que estado a tarefa nasce. Omitir qualquer uma das duas dá 400, não uma tarefa `PENDENTE` implícita. | confirmada | Decisão do mantenedor em 26/09/2026 |
| ASM-013 | A feature carrega as duas rotas — criação e consulta por id — embora o diretório se chame `003-create-task`. O nome fica como está porque a fila da constituição e a spec da 002 já o referenciam; renomear tornaria as duas referências obsoletas por ganho cosmético. | confirmada | Decisão do mantenedor em 26/09/2026 |
| ASM-014 | O `ErrorResponse` (e o par `field`/`message` dentro dele) vive em `presentation/handler`, junto de quem o produz, e não em `application/dto`: é forma de HTTP, não corpo de uma operação de aplicação. | confirmada | Confirmada em 26/09/2026: fica em `presentation/handler`, para que a camada de aplicação siga sem conhecer código de status |
| ASM-015 | As mensagens de validação de cada campo saem em português, como os valores dos enums (ASM-007 da 002). Chaves do JSON e códigos de erro continuam em inglês. | confirmada | Confirmada em 26/09/2026: mensagens em português, chaves e códigos em inglês |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-004 | Campo desconhecido no corpo da criação (por exemplo `id` ou `createdAt`) hoje é ignorado em silêncio, que é o padrão do Spring Boot. Recusar com 400 seria mais estrito e avisaria quem está integrando errado. | respondida | Continua ignorado, como o padrão do Spring Boot: `id`, `createdAt` e `updatedAt` têm uma fonte só, o servidor |
| Q-005 | `GET /api/tasks/abc` não casa o tipo do parâmetro e cai em "qualquer outra" da tabela de erros, virando 500 — que é errado, porque a culpa é do chamador. | respondida | Vira 400 `MALFORMED_REQUEST`, provado pela AC-020; a tabela de erros do CLAUDE.md ganha a linha correspondente |
