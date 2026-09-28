# Task Manager

Aplicação de gerenciamento de tarefas: criar, listar, buscar por título,
consultar por id, atualizar, alterar status e excluir. Monorepo com dois
artefatos independentes — API em Java 21 com Spring Boot e interface em
Angular — que se comunicam apenas por HTTP.

> **Estado atual:** o backend está completo — seis features implementadas e
> auditadas: a fundação
> ([`001-project-setup`](specs/features/001-project-setup/spec.md)), o domínio
> da tarefa com a porta de persistência
> ([`002-task-domain`](specs/features/002-task-domain/spec.md)), as duas
> primeiras rotas ([`003-create-task`](specs/features/003-create-task/spec.md)),
> a listagem com filtros
> ([`004-list-tasks`](specs/features/004-list-tasks/spec.md)), a edição
> ([`005-update-task`](specs/features/005-update-task/spec.md)) e a exclusão
> ([`006-delete-task`](specs/features/006-delete-task/spec.md)):
> `POST /api/tasks` cria, `GET /api/tasks` lista filtrando por título e
> situação, `GET /api/tasks/{id}` consulta, `PUT /api/tasks/{id}` substitui a
> tarefa inteira, `PATCH /api/tasks/{id}/status` troca só a situação e
> `DELETE /api/tasks/{id}` exclui. As seis rotas do CRUD existem; o que falta é
> a interface. Método que uma rota não aceita é 405 com o header `Allow`, e
> endereço que nem existe é 404 `RESOURCE_NOT_FOUND`.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA, Hibernate, Bean Validation |
| Banco | PostgreSQL 16, migrations com Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Frontend | Angular 22 (standalone components), TypeScript |
| Execução | Docker e Docker Compose, nginx servindo o build do frontend |
| Testes | JUnit 5, MockMvc, Testcontainers, ArchUnit, Playwright (ponta a ponta) |

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
| Testes de ponta a ponta | Node.js e Docker; uma vez por máquina, `cd e2e && npm ci && npx playwright install chromium` |

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

| Método | Rota | Sucesso | Estado |
|---|---|---|---|
| POST | `/api/tasks` | 201 + header `Location` | implementada |
| GET | `/api/tasks/{id}` | 200 | implementada |
| GET | `/api/tasks?title=&status=` | 200 | implementada |
| PUT | `/api/tasks/{id}` | 200 | implementada |
| PATCH | `/api/tasks/{id}/status` | 200 | implementada |
| DELETE | `/api/tasks/{id}` | 204 | implementada |

Criar uma tarefa — situação e prioridade são obrigatórias, e o header
`Location` da resposta é o endereço da tarefa criada:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{
        "title": "Escrever a spec da feature 004",
        "description": "Listagem com filtros de título e situação",
        "status": "PENDENTE",
        "priority": "ALTA",
        "dueDate": "2026-10-15"
      }'
```

Consultar a tarefa criada, pelo endereço que o `Location` devolveu:

```bash
curl -i http://localhost:8080/api/tasks/1
```

Listar, da tarefa mais recente para a mais antiga. Os dois filtros são
opcionais: `title` é trecho do título sem distinção de maiúsculas e `status` é
igualdade exata. Filtro em branco não restringe, e filtro que não casa nada é
uma lista vazia — não um erro:

```bash
curl 'http://localhost:8080/api/tasks'
curl 'http://localhost:8080/api/tasks?title=spec'
curl 'http://localhost:8080/api/tasks?title=spec&status=PENDENTE'
```

Situação fora de `PENDENTE`, `EM_ANDAMENTO` e `CONCLUIDA` é recusada com 400.

Substituir a tarefa inteira. Os cinco campos do corpo passam a ser os campos da
tarefa: campo opcional omitido (`description`, `dueDate`) apaga o valor gravado.
`createdAt` não muda, `updatedAt` avança, e prazo no passado é aceito aqui — ao
contrário da criação, porque corrigir o título de uma tarefa atrasada não pode
exigir mexer no prazo dela:

```bash
curl -i -X PUT http://localhost:8080/api/tasks/1 \
  -H 'Content-Type: application/json' \
  -d '{
        "title": "Escrever a spec da feature 005",
        "description": "Substituição e troca de situação",
        "status": "EM_ANDAMENTO",
        "priority": "ALTA",
        "dueDate": "2026-10-20"
      }'
```

Trocar a situação a partir da listagem, sem reenviar os outros campos:

```bash
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status": "CONCLUIDA"}'
```

Identificador que não existe é 404 `TASK_NOT_FOUND` nas duas rotas — `PUT` não
cria tarefa, porque quem gera o identificador é o banco.

Excluir uma tarefa — a resposta é 204 e não tem corpo:

```bash
curl -i -X DELETE http://localhost:8080/api/tasks/1
```

A exclusão é definitiva: não há lixeira nem desfazer. A segunda exclusão do
mesmo identificador é 404 `TASK_NOT_FOUND` — o efeito no servidor é o mesmo das
duas vezes, é só a resposta que difere. A exclusão é da tarefa, não da coleção:
`DELETE /api/tasks` continua respondendo 405.

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
campo rejeitado. Erro de protocolo tem a mesma forma: endereço que a API não
publica é 404 `RESOURCE_NOT_FOUND`, método não suportado é 405
`METHOD_NOT_ALLOWED` com o header `Allow` — nunca 500.

## Testes

As regras são provadas no backend:

```bash
cd backend && ./mvnw test
```

Exigem Docker: a persistência e a inicialização são exercitadas contra um
PostgreSQL real, subido por Testcontainers, para que as migrations rodem como
em produção. As regras de camada são verificadas com ArchUnit no mesmo build.

O caminho que o backend não alcança — navegador, nginx e o `proxy_pass` de
`/api/` — é provado de ponta a ponta com Playwright, no pacote
[`e2e/`](e2e/). Uma vez por máquina, instale o pacote e o navegador; depois
basta o compose de pé:

```bash
cd e2e && npm ci && npx playwright install chromium
cd e2e && npx playwright test
```

Não há teste unitário de componente Angular nesta versão — decisão consciente
de escopo, registrada em [`CLAUDE.md`](CLAUDE.md).

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

Cada critério de aceite (`AC-xxx`) precisa de um teste cujo título carregue
`@spec:AC-xxx` — o `@DisplayName` no backend, o nome do teste no Playwright.
Quem decide se o critério passou é o test runner, nunca uma afirmação em texto:
`scripts/spec-tap.mjs` roda o build do backend, sobe o compose com build, roda o
E2E, mescla os dois relatórios e imprime TAP, que é o formato que o motor lê.
Uma feature está pronta quando `audit --ci` sai com código 0.

Por isso **cada `verify` sobe e derruba o compose**: prova que depende de um
ambiente já de pé aprovaria uma imagem velha. Para pular o E2E em um `verify`
rápido, `SPEC_TAP_E2E=0` — os critérios de tela voltam sem prova, nunca com um
PASS que não aconteceu.

Os princípios inegociáveis do projeto estão em
[`specs/constituicao.md`](specs/constituicao.md); as features, em
[`specs/features/`](specs/features/).
