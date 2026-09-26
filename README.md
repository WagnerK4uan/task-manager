# Task Manager

Aplicação de gerenciamento de tarefas: criar, listar, buscar por título,
consultar por id, atualizar, alterar status e excluir. Monorepo com dois
artefatos independentes — API em Java 21 com Spring Boot e interface em
Angular — que se comunicam apenas por HTTP.

> **Estado atual:** a fundação (feature
> [`001-project-setup`](specs/features/001-project-setup/spec.md)) está
> implementada: o ambiente sobe inteiro por Docker, o schema é criado por
> migration e o contrato da API é publicado. Os endpoints de tarefa chegam
> nas features seguintes — hoje `/api/tasks` ainda responde 404.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Hibernate, Bean Validation |
| Banco | PostgreSQL 16, migrations com Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Frontend | Angular 22 (standalone components), TypeScript |
| Execução | Docker e Docker Compose, nginx servindo o build do frontend |
| Testes | JUnit 5, MockMvc, Testcontainers, ArchUnit |

## Arquitetura

Separação por camadas com as dependências apontando para dentro — o domínio
não conhece Spring, HTTP nem DTO:

```
presentation  ──▶  application  ──▶  domain  ◀──  infrastructure
 controller        service            entity       persistence
 handler           dto (records)      enums        configuration
                   mapper             exception
                                      repository (porta)
```

A persistência entra por uma porta declarada no domínio e implementada em
`infrastructure`, o que mantém o domínio livre do Spring Data e o service
testável sem framework de mock.

Essas regras não são convenção: `LayerDependencyTest` as declara com ArchUnit
e quebra o build quando alguém as viola.

O desenho completo, os padrões de código e as decisões arquiteturais estão em
[`CLAUDE.md`](CLAUDE.md).

## Pré-requisitos

Para subir tudo por Docker — o caminho recomendado — basta **Docker** com
Docker Compose v2. Nada de Java, Node ou PostgreSQL na máquina.

Para trabalhar em um dos módulos fora do container:

| Módulo | Exige |
|---|---|
| Backend | JDK 21 |
| Frontend | Node.js ≥ 22.22.3 (mínimo do Angular CLI 22) e npm |
| Testes do backend | Docker rodando — os testes de integração usam Testcontainers |

## Execução com Docker

```bash
cp .env.example .env     # preencha DB_USERNAME e DB_PASSWORD
docker compose up --build
```

| Serviço | URL |
|---|---|
| Interface | http://localhost:4200 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Documento OpenAPI | http://localhost:8080/v3/api-docs |
| Saúde da aplicação | http://localhost:8080/actuator/health |

O backend só inicia depois que o PostgreSQL passa no healthcheck, e o Flyway
aplica as migrations na subida. Os dados ficam em um volume nomeado e
sobrevivem a `docker compose down`; para descartá-los, `docker compose down -v`.

## Variáveis de ambiente

Nenhuma credencial é versionada: o repositório traz apenas o
[`.env.example`](.env.example). Faltando qualquer uma das três primeiras, o
`docker compose up` aborta na subida em vez de usar um valor embutido.

| Variável | Para que serve | Padrão |
|---|---|---|
| `DB_NAME` | nome do banco | — |
| `DB_USERNAME` | usuário do banco | — |
| `DB_PASSWORD` | senha do banco | — |
| `BACKEND_PORT` | porta da API na máquina host | `8080` |
| `FRONTEND_PORT` | porta da interface na máquina host | `4200` |

Fora do compose, o backend lê `DB_URL` (padrão
`jdbc:postgresql://localhost:5432/taskmanager`), `DB_USERNAME` e `DB_PASSWORD`.

## Execução local, módulo a módulo

Backend, com JDK 21 na máquina e um PostgreSQL acessível:

```bash
export DB_USERNAME=taskmanager DB_PASSWORD=...
cd backend && ./mvnw spring-boot:run
```

Sem JDK na máquina, o build acontece no mesmo container que a imagem usa:

```bash
docker run --rm --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -v "$PWD/backend":/app:Z -v "$HOME/.m2":/tmp/.m2:Z -w /app \
  maven:3.9-eclipse-temurin-21 mvn -Dmaven.repo.local=/tmp/.m2/repository test
```

Frontend, em modo de desenvolvimento:

```bash
cd frontend && npm install && npm start
```

O `ng serve` sobe em http://localhost:4200 e **não** encaminha `/api` — o
proxy para o backend vive no nginx da imagem. Para exercitar a integração,
use o compose.

## API

| Método | Rota | Sucesso |
|---|---|---|
| GET | `/api/tasks?title=&status=` | 200 |
| GET | `/api/tasks/{id}` | 200 |
| POST | `/api/tasks` | 201 + header `Location` |
| PUT | `/api/tasks/{id}` | 200 |
| PATCH | `/api/tasks/{id}/status` | 200 |
| DELETE | `/api/tasks/{id}` | 204 |

Criar uma tarefa:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
        "title": "Escrever a spec da feature 002",
        "description": "Entidade, enums e porta do repositório",
        "status": "PENDENTE",
        "priority": "ALTA",
        "dueDate": "2026-10-15"
      }'
```

Filtrar a listagem e trocar o status a partir dela:

```bash
curl 'http://localhost:8080/api/tasks?title=spec&status=PENDENTE'
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status": "CONCLUIDA"}'
```

Todo erro tem a mesma forma:

```json
{
  "timestamp": "2026-09-25T18:12:03Z",
  "status": 404,
  "error": "TASK_NOT_FOUND",
  "message": "Task not found",
  "path": "/api/tasks/10"
}
```

Erros de validação acrescentam `fields`, com um par `field`/`message` por
campo rejeitado.

## Testes

Os testes são do backend. O frontend não tem suíte automatizada nesta versão —
decisão consciente de escopo, registrada em [`CLAUDE.md`](CLAUDE.md).

```bash
cd backend && ./mvnw test
```

Exigem Docker: a persistência e a inicialização são exercitadas contra um
PostgreSQL real, subido por Testcontainers, para que as migrations rodem como
em produção. As regras de camada são verificadas com ArchUnit no mesmo build.

## Especificações e auditoria

Nenhuma funcionalidade significativa é implementada sem especificação, e a
especificação é auditada mecanicamente contra o código:

```bash
ONP=".claude/skills/onp-spec-driven/scripts/onp-spec.mjs"
node $ONP status                      # onde cada feature está
node $ONP audit                       # o que falta para spec e código baterem
node $ONP audit --ci                  # o gate: exit 0 = alinhado
node $ONP verify 001-project-setup    # roda os testes e grava a prova por critério
```

Cada critério de aceite (`AC-xxx`) precisa de um teste cujo `@DisplayName`
carregue `@spec:AC-xxx`. Quem decide se o critério passou é o test runner,
nunca uma afirmação em texto: `scripts/spec-tap.mjs` roda o build do backend,
lê os relatórios do Surefire e imprime TAP, que é o formato que o motor lê.
Uma feature está pronta quando `audit --ci` sai com código 0.

Os princípios inegociáveis do projeto estão em
[`specs/constituicao.md`](specs/constituicao.md); as features, em
[`specs/features/`](specs/features/).
