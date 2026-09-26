# Tasks: 001 — Configuração inicial do projeto

> feature: 001-project-setup

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida
-->

## T-001 — Esqueleto Maven do backend [pendente]

- Refs: US-001, AC-001
- Arquivos: backend/pom.xml, backend/mvnw, backend/mvnw.cmd, backend/.mvn/wrapper/maven-wrapper.properties, backend/src/main/java/com/example/taskmanager/TaskManagerApplication.java
- Notas: Java 21 via `<maven.compiler.release>21</maven.compiler.release>` (ASM-004). Dependências exatamente as listadas na spec, nenhuma a mais. Maven Wrapper versionado para que quem clona não precise de Maven instalado.

## T-002 — Perfis de configuração e datasource [pendente]

- Refs: AC-001, AC-003
- Arquivos: backend/src/main/resources/application.yml, backend/src/main/resources/application-docker.yml, specs/constituicao.md
- Notas: `ddl-auto: validate` nos dois perfis e Flyway habilitado. Credenciais só por variável de ambiente, sem valor padrão embutido em produção. Esta tarefa ativa P-002 e P-003 na constituição — os globs passam a casar arquivo no mesmo commit que os cria.

## T-003 — Migration inicial da tabela de tarefas [pendente]

- Refs: AC-002
- Arquivos: backend/src/main/resources/db/migration/V1__create_tasks_table.sql
- Notas: colunas e tipos exatamente como na tabela de "Regras de negócio". Enums como `varchar(20)`. Índice em `status` para o filtro da listagem (004); busca por título fica sem índice até existir volume que justifique.

## T-004 — Publicação do documento OpenAPI [pendente]

- Refs: AC-004
- Arquivos: backend/src/main/java/com/example/taskmanager/infrastructure/configuration/OpenApiConfig.java
- Notas: só título, descrição e versão da API. Springdoc descobre os endpoints sozinho — nenhuma anotação decorativa nos controllers.

## T-005 — Prova executável da fundação [pendente]

- Refs: AC-001, AC-002, AC-003, AC-004
- Arquivos: scripts/spec-tap.mjs, backend/src/test/java/com/example/taskmanager/PostgresIntegrationTest.java, backend/src/test/java/com/example/taskmanager/ProjectSetupTest.java
- Notas: `spec-tap.mjs` roda o build do backend, lê os XML do Surefire e imprime TAP com o `@DisplayName` de cada caso — é assim que o motor sabe critério a critério o que passou. `PostgresIntegrationTest` é a classe base com o container Postgres (ASM-001); os quatro critérios viram testes com `@spec:AC-00x` no `@DisplayName`.

## T-006 — Imagem Docker do backend [pendente]

- Refs: US-001
- Arquivos: backend/Dockerfile, backend/.dockerignore
- Notas: multi-stage (Maven + Temurin 21 no build, JRE 21 na imagem final), camada separada para as dependências e usuário sem privilégio de root na execução.

## T-007 — Orquestração com Docker Compose [pendente]

- Refs: US-001, AC-001
- Arquivos: docker-compose.yml, .env.example
- Notas: Postgres com volume nomeado e healthcheck; o backend só inicia com `depends_on: condition: service_healthy` (RNF-04). Nenhuma credencial literal no compose.

## T-008 — Esqueleto do frontend Angular [pendente]

- Refs: US-001
- Arquivos: frontend/package.json, frontend/angular.json, frontend/tsconfig.json, frontend/src/main.ts, frontend/src/index.html, frontend/src/styles.css, frontend/src/app/app.ts, frontend/src/app/app.html, frontend/src/app/app.css, frontend/src/app/app.config.ts, frontend/src/app/app.routes.ts, frontend/Dockerfile, frontend/nginx.conf, frontend/.dockerignore
- Notas: `ng new --standalone --routing --style=css --skip-tests` (sem testes de frontend por decisão registrada no CLAUDE.md). Build servido por nginx com fallback de SPA e proxy de `/api` para o backend. A lista de arquivos acima segue o que o Angular CLI gera na versão instalada — se divergir, o audit acusa como código órfão e a lista é corrigida aqui.

## T-009 — README de execução [pendente]

- Refs: US-001, US-002
- Arquivos: README.md
- Notas: descrição, stack, arquitetura, pré-requisitos, execução local e por Docker, variáveis de ambiente, endpoints, exemplos de request, como rodar os testes e a auditoria de specs.
