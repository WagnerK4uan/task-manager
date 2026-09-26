# Task Manager

Contexto principal do projeto para agentes de código. Especificações de feature
**não** moram aqui — elas vivem em `specs/features/`.

## Visão geral

Aplicação de gerenciamento de tarefas: criar, listar, buscar por título,
consultar por id, atualizar, alterar status e excluir. Monorepo com dois
artefatos independentes que se comunicam apenas por HTTP.

A regra que governa todas as decisões abaixo:

> **Complexidade só com justificativa técnica.** Uma abstração que não resolve
> um problema real do projeto é dívida, não qualidade.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Hibernate, Bean Validation |
| Banco | PostgreSQL 16, migrations com Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Frontend | Angular (standalone components), TypeScript, Reactive Forms |
| Build | Maven Wrapper (backend), npm (frontend) |
| Execução | Docker e Docker Compose |
| Testes | JUnit 5, Mockito, MockMvc, Testcontainers, ArchUnit |

O Spring Boot 4 renomeou starters: use `spring-boot-starter-webmvc` (não
`-web`) e `spring-boot-starter-flyway`. O antigo `spring-boot-starter-test` foi
quebrado em fatias por tecnologia (`-webmvc-test`, `-data-jpa-test`,
`-flyway-test`, `-actuator-test`).

## Arquitetura

Separação por camadas com as dependências apontando para dentro:

```
presentation  ──▶  application  ──▶  domain  ◀──  infrastructure
 controller        service            entity       persistence
 handler           dto (records)      enums        configuration
                   mapper             exception
                                      repository (porta)
```

- `domain` não importa Spring, não importa DTO, não conhece HTTP.
- `infrastructure` depende de `domain` porque **implementa** as portas dele.
- `presentation` conhece `application`; nunca toca em repositório nem em
  entidade.
- A entidade JPA vive em `domain` e carrega as anotações de mapeamento. É Clean
  Architecture pragmática: separar modelo de domínio e modelo de persistência
  exigiria um terceiro conjunto de classes e mappers para oito campos.
- **Essas regras são teste, não convenção.**
  `backend/src/test/java/com/taskmanager/architecture/LayerDependencyTest.java`
  as declara com ArchUnit e quebra o build quando alguém as viola.

**Porta e adaptador na persistência.** `domain/repository/TaskRepository` é uma
interface própria, com apenas os métodos que o service usa.
`infrastructure/persistence/TaskRepositoryAdapter` a implementa delegando para
`TaskJpaRepository` (Spring Data). O custo são duas classes pequenas; o ganho é
que o domínio não depende do Spring Data e o service é testável com um duplo em
memória, sem framework de mock.

## Estrutura de diretórios

```
.
├── CLAUDE.md                  # este arquivo
├── README.md
├── docker-compose.yml         # postgres + backend + frontend
├── .env.example
├── onpspec.config.json        # configuração do motor de specs
├── scripts/spec-tap.mjs       # relatórios do Surefire -> TAP (prova por critério)
├── specs/                     # constituição, features e provas
│   ├── constituicao.md
│   ├── features/<nnn>-<nome>/{spec.md,tasks.md}
│   └── verification/<feature>.json
├── backend/
│   └── src/main/java/com/taskmanager/
│       ├── domain/{entity,enums,exception,repository}
│       ├── application/{dto,mapper,service}
│       ├── infrastructure/{persistence,configuration}
│       └── presentation/{controller,handler}
└── frontend/
    └── src/app/
        ├── core/{services,interceptors}
        ├── features/tasks/{pages,components,models}
        └── shared/
```

## Regras de desenvolvimento

Proibido, sem exceção:

- lógica de negócio em controller;
- controller acessando repositório;
- entidade JPA como payload de entrada ou saída da API;
- `try/catch` espalhado em controller ou service para virar resposta HTTP — o
  `@RestControllerAdvice` é o único lugar que traduz exceção em status;
- dependência nova sem justificativa registrada na spec da feature;
- padrão arquitetural introduzido sem um problema concreto que ele resolva;
- alterar arquivo que não pertence à tarefa em andamento.

Antes de mudar algo relevante: leia o código existente, leia a spec da feature,
identifique o impacto, proponha a abordagem e só então implemente.

## Padrões de código

- **DTOs são `record`** e ficam em `application/dto`. Um DTO por operação, só
  com os campos daquela operação.
- **Sem Lombok e sem MapStruct.** A entidade tem acessores explícitos; o
  `TaskMapper` é uma classe com métodos simples.
- **Datas:** `Instant` (UTC) para `createdAt`/`updatedAt`, preenchidos por
  `@CreationTimestamp` e `@UpdateTimestamp`; `LocalDate` para `dueDate`.
  Uma escolha só, em toda a aplicação.
- **Enums persistidos como texto** (`@Enumerated(EnumType.STRING)`). Ordinal
  nunca: reordenar o enum corromperia dados gravados.
- **Consultas:** derivadas do Spring Data ou uma `@Query` JPQL legível.
  Specification/Criteria API só se a combinação de filtros crescer a ponto de
  tornar a JPQL ilegível.
- **Nomes descritivos** e métodos curtos. Comentário só para explicar decisão
  não óbvia — nunca para narrar o que o código já diz, e nunca para justificar
  código complicado que podia ser simples.
- **Validação** com Bean Validation nos DTOs de entrada (`@NotBlank`, `@Size`,
  `@NotNull`, `@FutureOrPresent`), ativada por `@Valid` no controller.

### Contrato de erro da API

Uma forma só, para todo erro:

```json
{
  "timestamp": "2026-09-25T18:12:03Z",
  "status": 404,
  "error": "TASK_NOT_FOUND",
  "message": "Task not found",
  "path": "/api/tasks/10"
}
```

Erros de validação acrescentam `fields`, com um par `field`/`message` por campo
rejeitado. Mapeamentos no `GlobalExceptionHandler`:

| Exceção | Status | `error` |
|---|---|---|
| `TaskNotFoundException` | 404 | `TASK_NOT_FOUND` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST` |
| `MethodArgumentTypeMismatchException` | 400 | `MALFORMED_REQUEST` |
| qualquer outra | 500 | `INTERNAL_ERROR` (sem vazar stack trace) |

### Endpoints

| Método | Rota | Sucesso |
|---|---|---|
| GET | `/api/tasks?title=&status=` | 200 |
| GET | `/api/tasks/{id}` | 200 |
| POST | `/api/tasks` | 201 + header `Location` |
| PUT | `/api/tasks/{id}` | 200 |
| PATCH | `/api/tasks/{id}/status` | 200 |
| DELETE | `/api/tasks/{id}` | 204 |

`PUT` substitui a tarefa inteira, inclusive o status, porque o formulário de
edição tem o campo. `PATCH /status` existe para a troca rápida a partir da
listagem. É a diferença entre substituição e modificação parcial — não é
redundância.

## Fluxo de trabalho: Spec-Driven Development

Nenhuma funcionalidade significativa é implementada sem especificação. O ciclo:

```
Requisito → Especificação → Design → Implementação → Verificação → Auditoria
```

1. Analisar o requisito.
2. Criar ou atualizar `specs/features/<nnn>-<nome>/spec.md`.
3. Definir os critérios de aceite (o que um teste consegue observar).
4. Quebrar em tarefas no `tasks.md`, com `Refs:` e `Arquivos:`.
5. Implementar — uma tarefa, um commit.
6. Rodar os testes e registrar a prova (`verify`).
7. Auditar contra a especificação (`audit --ci`).
8. Só então a tarefa está concluída.

Decisão arquitetural relevante ou ambiguidade aparecem **antes** da
implementação, registradas como suposição (ASM-xxx) ou pergunta em aberto
(Q-xxx) na spec.

### O gate é mecânico

O projeto usa o motor `onp-spec` (embarcado em `.claude/skills/onp-spec-driven/`).
Ele cruza especificação, tarefas e testes e responde por código de saída:

- cada critério de aceite (AC-xxx) precisa de um teste cujo `@DisplayName`
  contenha `@spec:AC-xxx`;
- quem decide se o critério passou é o test runner, nunca uma afirmação em
  texto;
- teste pulado não é prova;
- uma feature está pronta quando `audit --ci` sai com código 0.

`scripts/spec-tap.mjs` roda o build do backend, lê os XML do Surefire e imprime
TAP — é o formato que o motor lê para saber, critério a critério, o que passou.

### Escrevendo uma spec

Cada `spec.md` tem, nesta ordem: **Objetivo**, **Contexto**, **Requisitos
funcionais** (como histórias US-xxx e critérios AC-xxx em Dado/Quando/Então),
**Requisitos não funcionais**, **Regras de negócio**, **Casos de erro**,
**Impacto técnico**, **Dependências**, **Fora de escopo**, **Suposições** e
**Perguntas em aberto**.

As duas últimas são obrigatórias: se não houver nenhuma, escreva "Nenhuma." —
e desconfie. Códigos de rastreio têm no mínimo três dígitos e são únicos no
projeto inteiro; nunca reutilize um número.

### Constituição

`specs/constituicao.md` guarda os princípios inegociáveis. Todo princípio
[DEVE] tem verificação executável — em geral um regex proibido sobre um glob.
Um princípio só entra quando o glob dele casa algum arquivo; a fila, no fim do
arquivo, diz em qual feature cada um é ativado. Princípio violado se conserta
no código, nunca enfraquecendo o princípio.

## Testes

- **Escopo:** backend. O frontend não tem testes automatizados nesta versão —
  decisão consciente de escopo, registrada aqui para não parecer esquecimento.
- **Service:** unitário, com um duplo em memória da porta `TaskRepository`.
  Sem subir contexto Spring.
- **Controller:** `@WebMvcTest` com o service mockado — valida status, corpo e
  serialização.
- **Persistência e inicialização:** Testcontainers com Postgres real, para que
  as migrations Flyway sejam exercitadas exatamente como em produção.
- **Camadas:** ArchUnit. A regra de dependência entre camadas é um teste que
  falha o build, não um acordo verbal.
- Cada critério de aceite tem pelo menos um teste, anotado assim:

```java
@Test
@DisplayName("@spec:AC-003 recusa tarefa sem título")
void recusaTarefaSemTitulo() { ... }
```

- O teste verifica o resultado descrito na especificação, não o formato interno
  do código. Nunca enfraqueça, pule ou apague um teste para o gate passar.

## Git

- **Commits são feitos exclusivamente pela pessoa mantenedora do projeto.**
  Agentes de código deixam os arquivos prontos no diretório de trabalho e
  propõem a mensagem de commit — nunca executam `git commit`, `git push`,
  `git reset` ou qualquer comando que altere o histórico.
- A autoria dos commits é única: nada de linhas `Co-Authored-By`.
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`,
  `chore:`.
- **Uma tarefa, um commit.** Mensagens de implementação citam a tarefa:
  `feat: add task creation endpoint (T-003)`.
- Commits pequenos e coesos. Nada de commit gigante com funcionalidades não
  relacionadas.
- A mensagem descreve a mudança, não o processo.

## Comandos

```bash
# motor de specs (a partir da raiz do repositório)
ONP=".claude/skills/onp-spec-driven/scripts/onp-spec.mjs"
node $ONP status                      # onde cada feature está
node $ONP audit                       # o que falta para a spec e o código baterem
node $ONP audit --ci                  # o gate: exit 0 = alinhado
node $ONP verify 001-project-setup    # roda os testes e grava a prova por critério
node $ONP scaffold 001-project-setup  # gera o esqueleto de teste que falha
node $ONP assumptions                 # o que o projeto está assumindo

# backend — com JDK 21 na máquina
cd backend && ./mvnw test             # testes (exigem Docker para os Testcontainers)
cd backend && ./mvnw spring-boot:run  # aplicação local

# backend — sem JDK na máquina, build no container (mesmo caminho do Dockerfile)
docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$PWD/backend":/app:Z -v "$HOME/.m2":/tmp/.m2:Z -w /app \
  maven:3.9-eclipse-temurin-21 mvn -Dmaven.repo.local=/tmp/.m2/repository test

# frontend
cd frontend && npm start

# ambiente completo
docker compose up --build
```

## Decisões arquiteturais

| # | Decisão | Motivo |
|---|---|---|
| D-1 | Specs auditadas mecanicamente (`onp-spec`) em vez de markdown solto | especificação sem gate vira ficção assim que o código evolui; aqui o desalinhamento tem código de saída |
| D-2 | Porta no domínio + adaptador em infrastructure | inverte a dependência de verdade e torna o service testável sem framework de mock, ao custo de duas classes |
| D-3 | Testes só no backend | é onde estão as regras; cobrir o frontend exigiria infraestrutura de teste que não paga o próprio custo nesta versão |
| D-4 | Prova via TAP gerado do Surefire | o motor precisa saber critério a critério o que passou; usar só o código de saída global provaria demais |
| D-5 | `Instant` em UTC para auditoria, `LocalDate` para vencimento | vencimento é um dia civil, não um instante; misturar os dois tipos é origem clássica de erro de fuso |
| D-6 | Enum como texto no banco | reordenar o enum no código não pode corromper dados |
| D-7 | Sem Lombok, sem MapStruct | `record` resolve o boilerplate onde ele dói; o resto não justifica um processador de anotações |
| D-8 | Sem paginação na listagem | não é requisito; entra quando houver volume que justifique |
| D-9 | Testcontainers em vez de H2 | as migrations são específicas de Postgres; testar contra H2 provaria um schema que não é o de produção |
| D-10 | Regras de camada com ArchUnit | "seguimos Clean Architecture" só é verdade se algo verificar; o teste transforma a regra em gate e custa uma dependência de teste |
| D-11 | Spring Boot 4.1 | a linha 3.x saiu de suporte e não é mais oferecida pelo Initializr; fixar versão sem correções é dívida nascendo pronta |
| D-12 | Pacote base `com.taskmanager` | o pacote nomeia o domínio, não a camada onde o código roda |
