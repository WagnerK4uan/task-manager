# Tasks: 001 — Configuração inicial do projeto

> feature: 001-project-setup

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  As tarefas estão na ordem de execução, não na ordem numérica: T-010 nasceu
  depois das outras, mas precisa entrar no começo, porque é ela que guarda a
  arquitetura enquanto o código chega. Códigos de rastreio nunca são
  renumerados nem reaproveitados.
-->

## T-001 — Módulo Maven alinhado ao alvo [pendente]

- Refs: US-001, AC-001, AC-005
- Arquivos: backend/pom.xml, backend/src/main/java/com/taskmanager/TaskManagerApplication.java
- Notas: parte do esqueleto do Initializr (ASM-005). Coordenadas para `com.taskmanager:task-manager`, pacote base `com.taskmanager`, `java.version` 21, driver PostgreSQL no lugar do MySQL, Lombok e os dois blocos de `annotationProcessorPaths` fora, metadados vazios removidos. Entram as dependências que faltam: validation, flyway, springdoc, actuator, testcontainers e archunit. Apagar `backend/src/main/java/com/backend/backend/BackendApplication.java`, `backend/src/test/java/com/backend/backend/BackendApplicationTests.java` e `backend/HELP.md`. Fecha compilando: `mvn clean compile` com `release 21`.

## T-002 — Perfis de configuração e datasource [pendente]

- Refs: AC-001, AC-003
- Arquivos: backend/src/main/resources/application.yml, backend/src/main/resources/application-docker.yml, specs/constituicao.md
- Notas: substitui o `application.properties` gerado. `ddl-auto: validate` nos dois perfis e Flyway habilitado. Credenciais só por variável de ambiente, sem valor padrão embutido no perfil de container. Esta tarefa ativa P-002 e P-003 na constituição — os globs passam a casar arquivo no mesmo commit que os cria.

## T-010 — Regras de camada verificadas por teste [pendente]

- Refs: US-003, AC-005, AC-006, AC-007
- Arquivos: backend/src/test/java/com/taskmanager/architecture/LayerDependencyTest.java
- Notas: três regras ArchUnit, uma por critério, com `@spec:AC-00x` no `@DisplayName`. Enquanto os pacotes das camadas estiverem vazios, `allowEmptyShould(true)` mantém a regra válida sem precisar reescrevê-la depois (ASM-006). A regra do domínio tolera `jakarta.persistence` e nada mais de framework. Entra logo no começo: é a rede que segura as features seguintes.

## T-003 — Migration inicial da tabela de tarefas [pendente]

- Refs: AC-002
- Arquivos: backend/src/main/resources/db/migration/V1__create_tasks_table.sql
- Notas: colunas e tipos exatamente como na tabela de "Regras de negócio". Enums como `varchar(20)`. Índice em `status` para o filtro da listagem (004); busca por título fica sem índice até existir volume que justifique.

## T-004 — Publicação do documento OpenAPI [pendente]

- Refs: AC-004
- Arquivos: backend/src/main/java/com/taskmanager/infrastructure/configuration/OpenApiConfig.java
- Notas: só título, descrição e versão da API. Springdoc descobre os endpoints sozinho — nenhuma anotação decorativa nos controllers.

## T-005 — Prova executável da fundação [pendente]

- Refs: AC-001, AC-002, AC-003, AC-004
- Arquivos: scripts/spec-tap.mjs, backend/src/test/java/com/taskmanager/PostgresIntegrationTest.java, backend/src/test/java/com/taskmanager/ProjectSetupTest.java
- Notas: `spec-tap.mjs` usa o `./mvnw` local quando existe um JDK e cai para o container Maven quando não existe (ASM-004); roda o build, lê os XML do Surefire e imprime TAP com o `@DisplayName` de cada caso — é assim que o motor sabe critério a critério o que passou. `PostgresIntegrationTest` é a classe base com o container PostgreSQL (ASM-001); os quatro critérios de US-001 e US-002 viram testes anotados.

## T-006 — Imagem Docker do backend [pendente]

- Refs: US-001
- Arquivos: backend/Dockerfile, backend/.dockerignore
- Notas: multi-stage (Maven + Temurin 21 no build, JRE 21 na imagem final), camada separada para as dependências e usuário sem privilégio de root na execução.

## T-007 — Orquestração com Docker Compose [pendente]

- Refs: US-001, AC-001
- Arquivos: docker-compose.yml, .env.example
- Notas: PostgreSQL com volume nomeado e healthcheck; o backend só inicia com `depends_on: condition: service_healthy` (RNF-04). Nenhuma credencial literal no compose.

## T-008 — Esqueleto do frontend Angular [pendente]

- Refs: US-001
- Arquivos: frontend/package.json, frontend/angular.json, frontend/tsconfig.json, frontend/src/main.ts, frontend/src/index.html, frontend/src/styles.css, frontend/src/app/app.ts, frontend/src/app/app.html, frontend/src/app/app.css, frontend/src/app/app.config.ts, frontend/src/app/app.routes.ts, frontend/Dockerfile, frontend/nginx.conf, frontend/.dockerignore
- Notas: `ng new --standalone --routing --style=css --skip-tests` (sem testes de frontend por decisão registrada no CLAUDE.md). Build servido por nginx com fallback de SPA e proxy de `/api` para o backend. A lista de arquivos acima segue o que o Angular CLI gera na versão instalada — se divergir, o audit acusa como código órfão e a lista é corrigida aqui.

## T-009 — README de execução [pendente]

- Refs: US-001, US-002
- Arquivos: README.md
- Notas: descrição, stack, arquitetura, pré-requisitos, execução local e por Docker, variáveis de ambiente, endpoints, exemplos de request, como rodar os testes e a auditoria de specs.
