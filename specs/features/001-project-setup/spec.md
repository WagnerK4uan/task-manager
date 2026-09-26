# Spec: 001 — Configuração inicial do projeto

> feature: 001-project-setup
> status: pronta

<!--
  US-xxx = história de usuário · AC-xxx = critério de aceite
  ASM-xxx = suposição · Q-xxx = pergunta em aberto
  Todo critério de aceite vira um teste cujo título carrega @spec:AC-xxx.
-->

## Objetivo

Deixar o monorepo executável de ponta a ponta — backend Spring Boot, banco
Postgres em container e esqueleto Angular — com o schema versionado por
migration e o contrato da API navegável, sem que nenhuma regra de negócio de
tarefa exista ainda.

## Contexto

O Task Manager é um monorepo com dois artefatos independentes (`backend/` em
Java 21 + Maven, `frontend/` em Angular) que se comunicam só por HTTP. Quem
clona o repositório precisa de um caminho único e curto para ter o sistema no
ar; quem for implementar as features seguintes precisa de uma base onde
migration, perfis de configuração e empacotamento já estejam decididos.

Esta é a feature de fundação: tudo o que vem depois (002 em diante) assume que
o banco sobe, as migrations rodam e o contexto Spring carrega.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-001 — Ambiente reproduzível em um comando

Como pessoa desenvolvedora, quero subir a aplicação e o banco com um comando
só, para começar a trabalhar sem instalar nem configurar Postgres na máquina.

#### AC-001 — A aplicação conecta a um Postgres real

- **Dado** um Postgres disponível com as credenciais do ambiente
- **Quando** a aplicação inicializa
- **Então** o contexto sobe sem erro e a conexão com o banco responde

#### AC-002 — As migrations criam a tabela de tarefas

- **Dado** um banco vazio
- **Quando** a aplicação inicializa e o Flyway aplica as migrations
- **Então** existe a tabela `tasks` com as colunas `id`, `title`,
  `description`, `status`, `priority`, `due_date`, `created_at` e `updated_at`
  nos tipos definidos em "Regras de negócio"

#### AC-003 — O schema nunca é gerado pelo ORM

- **Dado** a aplicação configurada em qualquer perfil
- **Quando** o contexto de persistência é inicializado
- **Então** o Hibernate está em modo `validate` e falha se o schema divergir
  das migrations, em vez de criar ou alterar tabelas

### US-002 — Contrato da API navegável

Como pessoa que integra com a API, quero abrir a documentação no navegador,
para entender os endpoints e os formatos sem precisar ler o código.

#### AC-004 — O documento OpenAPI é publicado pela aplicação

- **Dado** a aplicação no ar
- **Quando** é feita uma requisição a `/v3/api-docs`
- **Então** a resposta é 200 com um documento OpenAPI válido que identifica a
  API pelo título e pela versão do projeto

## Requisitos não funcionais

- **RNF-01** — Subir o ambiente do zero exige apenas Docker e um comando
  (`docker compose up`); nada de instalar Java, Node ou Postgres na máquina.
- **RNF-02** — A imagem do backend é multi-stage: o JDK e o Maven ficam no
  estágio de build, e a imagem final carrega só o JRE e o jar.
- **RNF-03** — Nenhuma credencial é versionada. O compose lê variáveis de
  ambiente, e o repositório versiona apenas `.env.example`.
- **RNF-04** — O backend só é considerado no ar depois que o Postgres passa no
  healthcheck do compose — a ordem de inicialização não pode depender de sorte.
- **RNF-05** — O build do backend roda offline depois do primeiro download
  (dependências resolvidas em camada própria da imagem).

## Regras de negócio

Esta feature não implementa comportamento de tarefa. Ela fixa o contrato de
dados que as features seguintes vão usar:

| Coluna | Tipo no banco | Tipo em Java | Nulo? |
|---|---|---|---|
| `id` | `bigserial` (PK) | `Long` | não |
| `title` | `varchar(120)` | `String` | não |
| `description` | `varchar(2000)` | `String` | sim |
| `status` | `varchar(20)` | `TaskStatus` | não |
| `priority` | `varchar(20)` | `TaskPriority` | não |
| `due_date` | `date` | `LocalDate` | sim |
| `created_at` | `timestamptz` | `Instant` | não |
| `updated_at` | `timestamptz` | `Instant` | não |

- Enums são persistidos como texto, nunca como posição ordinal: reordenar o
  enum no código não pode corromper dados já gravados.
- Toda data e hora de auditoria é gravada em UTC (`Instant`); `due_date` é
  data civil sem fuso, porque o vencimento de uma tarefa é um dia, não um
  instante.

## Casos de erro

| Situação | Comportamento esperado |
|---|---|
| Banco indisponível na inicialização | A aplicação falha rápido, com mensagem apontando a URL do datasource — não sobe degradada |
| Schema divergente das migrations | O Hibernate em `validate` aborta a inicialização indicando a coluna divergente |
| Migration já aplicada com checksum diferente | O Flyway aborta a inicialização; migration aplicada não se edita, se corrige com uma nova |
| Variável de ambiente de credencial ausente | O compose falha na subida em vez de usar um valor padrão embutido |

## Impacto técnico

- Cria os dois módulos do monorepo e o empacotamento Docker de ambos.
- Fixa decisões que todas as features seguintes herdam: `Instant` em UTC para
  auditoria, enum como texto, migration como única fonte do schema.
- Liga o gate mecânico do SDD ao build Java: `scripts/spec-tap.mjs` traduz os
  relatórios do Surefire para TAP, que é o formato que o motor de auditoria lê.
- Ativa na constituição os princípios P-002 (schema versionado) e P-003
  (segredos fora do código), que até aqui estavam na fila por falta de
  arquivos para verificar.

## Dependências

- **Externas:** Docker e Docker Compose na máquina de quem executa.
- **Bloqueia:** 002-task-domain e todas as features seguintes.
- **Depende de:** nada — é a primeira feature.
- **Bibliotecas** (cada uma com a razão de existir):

| Dependência | Por que entra |
|---|---|
| `spring-boot-starter-web` | expor a API REST |
| `spring-boot-starter-data-jpa` | mapeamento objeto-relacional |
| `spring-boot-starter-validation` | validação declarativa dos DTOs (features seguintes) |
| `postgresql` (driver) | acesso ao banco |
| `flyway-core` + `flyway-database-postgresql` | migrations versionadas |
| `springdoc-openapi-starter-webmvc-ui` | publicar o documento OpenAPI e o Swagger UI (AC-004) |
| `spring-boot-starter-actuator` | endpoint de saúde usado pelo healthcheck do compose (RNF-04) |
| `spring-boot-starter-test` | JUnit 5, AssertJ, MockMvc |
| `testcontainers` + `spring-boot-testcontainers` | Postgres real nos testes de integração (ASM-001) |

Não entram: Lombok (DTOs são `record` e a entidade tem acessores explícitos —
evita um processador de anotações e um plugin de IDE), MapStruct (mapear oito
campos não justifica gerar código) e H2 (ver ASM-001).

## Fora de escopo

- Qualquer endpoint de tarefa (`/api/tasks`) — isso é da 003 em diante.
- Entidade, enums e repositório do domínio — isso é da 002.
- Qualquer tela ou componente Angular além do esqueleto gerado pelo CLI.
- Autenticação, autorização e perfis de usuário (ASM-003).
- Pipeline de CI, deploy e observabilidade além do endpoint de saúde.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-001 | Os testes de integração usam Testcontainers com Postgres, não H2: as migrations são específicas de Postgres (`timestamptz`, `bigserial`) e um teste contra H2 provaria um schema que não é o de produção. Custo aceito: `mvn test` passa a exigir Docker rodando. | confirmada | Confirmada: Testcontainers com Postgres; Docker passa a ser pré-requisito de `./mvnw test` |
| ASM-002 | Não existe máquina de estados de status: qualquer transição é permitida. Se o domínio exigir transições restritas, isso vira regra de negócio da 006. | confirmada | Confirmada: sem máquina de estados nesta versão |
| ASM-003 | A aplicação não tem autenticação nem multiusuário: todas as tarefas pertencem a um único espaço compartilhado. | confirmada | Confirmada: aplicação sem autenticação |
| ASM-004 | O `pom.xml` fixa `release 21` mesmo com um JDK mais novo instalado na máquina, e a imagem Docker usa Temurin 21 — o bytecode entregue é sempre Java 21. | confirmada | Confirmada: `release 21` no pom e Temurin 21 na imagem |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-001 | Nenhuma. As decisões de arquitetura, fluxo SDD, camada de persistência, escopo de testes e formato de prova foram decididas antes desta especificação e estão registradas no CLAUDE.md. | respondida | Registradas em CLAUDE.md, seção "Decisões arquiteturais" |
