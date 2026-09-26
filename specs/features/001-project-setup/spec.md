# Spec: 001 — Configuração inicial do projeto

> feature: 001-project-setup
> status: auditada

<!--
  US-xxx = história de usuário · AC-xxx = critério de aceite
  ASM-xxx = suposição · Q-xxx = pergunta em aberto
  Todo critério de aceite vira um teste cujo título carrega @spec:AC-xxx.
-->

## Objetivo

Deixar o monorepo executável de ponta a ponta — backend Spring Boot, banco
PostgreSQL em container e esqueleto Angular — com o schema versionado por
migration, o contrato da API navegável e a regra de dependência entre camadas
verificada por teste, sem que nenhuma regra de negócio de tarefa exista ainda.

## Contexto

O Task Manager é um monorepo com dois artefatos independentes (`backend/` em
Java 21 + Maven, `frontend/` em Angular) que se comunicam só por HTTP. Quem
clona o repositório precisa de um caminho único e curto para ter o sistema no
ar; quem for implementar as features seguintes precisa de uma base onde
migration, perfis de configuração, empacotamento e limites de camada já estejam
decididos e verificados.

**Ponto de partida real:** o módulo `backend/` já existe, gerado pelo Spring
Initializr com Spring Boot 4.1.1. O esqueleto trouxe escolhas que divergem dos
requisitos do projeto — driver MySQL, Java 17, Lombok, coordenadas
`com.backend:backend` — e não trouxe Flyway, validação, OpenAPI, actuator nem
Testcontainers. Esta feature parte desse esqueleto e o alinha ao alvo; não
recria o módulo do zero.

Esta é a feature de fundação: tudo o que vem depois (002 em diante) assume que o
banco sobe, as migrations rodam, o contexto Spring carrega e a arquitetura é
guardada por teste.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-001 — Ambiente reproduzível em um comando

Como pessoa desenvolvedora, quero subir a aplicação e o banco com um comando
só, para começar a trabalhar sem instalar nem configurar PostgreSQL na máquina.

#### AC-001 — A aplicação conecta a um PostgreSQL real

- **Dado** um PostgreSQL disponível com as credenciais do ambiente
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

### US-003 — Arquitetura que não se degrada sozinha

Como pessoa que mantém o sistema, quero que a regra de dependência entre as
camadas seja verificada a cada build, para que a separação não dependa de
disciplina nem de revisão manual.

#### AC-005 — O domínio não depende de framework nem das outras camadas

- **Dado** as classes compiladas do backend
- **Quando** as regras de camada são verificadas
- **Então** nenhuma classe de `domain` depende de `org.springframework`, de
  `application`, de `infrastructure` ou de `presentation` — a única dependência
  de framework tolerada é `jakarta.persistence`, pelo mapeamento da entidade

#### AC-006 — A apresentação não conhece entidade nem repositório

- **Dado** as classes compiladas do backend
- **Quando** as regras de camada são verificadas
- **Então** nenhuma classe de `presentation` depende de `domain.entity`, de
  `domain.repository` ou de `infrastructure.persistence`

#### AC-007 — A aplicação não depende de quem a chama nem de quem a serve

- **Dado** as classes compiladas do backend
- **Quando** as regras de camada são verificadas
- **Então** nenhuma classe de `application` depende de `presentation` nem de
  `infrastructure`

## Requisitos não funcionais

- **RNF-01** — Subir o ambiente do zero exige apenas Docker e um comando
  (`docker compose up`); nada de instalar Java, Node ou PostgreSQL na máquina.
- **RNF-02** — A imagem do backend é multi-stage: o JDK e o Maven ficam no
  estágio de build, e a imagem final carrega só o JRE e o jar.
- **RNF-03** — Nenhuma credencial é versionada. O compose lê variáveis de
  ambiente, e o repositório versiona apenas `.env.example`.
- **RNF-04** — O backend só é considerado no ar depois que o PostgreSQL passa no
  healthcheck do compose — a ordem de inicialização não pode depender de sorte.
- **RNF-05** — O bytecode entregue é Java 21, independente do JDK da máquina de
  quem constrói.
- **RNF-06** — A violação de uma regra de camada quebra o build, não uma revisão
  de código.

## Regras de negócio

Esta feature não implementa comportamento de tarefa. Ela fixa o contrato de
dados e o desenho de pacotes que as features seguintes vão usar.

Contrato de dados da tabela `tasks`:

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
- Toda data e hora de auditoria é gravada em UTC (`Instant`); `due_date` é data
  civil sem fuso, porque o vencimento de uma tarefa é um dia, não um instante.

Desenho de pacotes, com a direção das dependências apontando para dentro:

```
com.taskmanager
├── domain          entity · enums · exception · repository (porta)
├── application     dto (records) · mapper · service
├── infrastructure  persistence (adapter + Spring Data) · configuration
└── presentation    controller · handler
```

- `domain` é o centro: não conhece Spring, DTO nem HTTP.
- `infrastructure` depende de `domain` porque **implementa** as portas dele.
- `presentation` conversa só com `application`.
- A entidade JPA vive em `domain` e carrega as anotações de mapeamento. É Clean
  Architecture pragmática: separar modelo de domínio e modelo de persistência
  exigiria um terceiro conjunto de classes e mappers para oito campos, o que
  contraria a regra de complexidade só com justificativa.

## Casos de erro

| Situação | Comportamento esperado |
|---|---|
| Banco indisponível na inicialização | A aplicação falha rápido, com mensagem apontando a URL do datasource — não sobe degradada |
| Schema divergente das migrations | O Hibernate em `validate` aborta a inicialização indicando a coluna divergente |
| Migration já aplicada com checksum diferente | O Flyway aborta a inicialização; migration aplicada não se edita, se corrige com uma nova |
| Variável de ambiente de credencial ausente | O compose falha na subida em vez de usar um valor padrão embutido |
| Classe colocada na camada errada | O teste de regras de camada falha e o build para, apontando a classe e a dependência proibida |

## Impacto técnico

Correções no esqueleto gerado pelo Initializr (ASM-005):

| O que veio | O que passa a valer | Por quê |
|---|---|---|
| `com.mysql:mysql-connector-j` | `org.postgresql:postgresql` | o projeto usa PostgreSQL; `timestamptz` e `bigserial` são dele |
| `java.version` 17 | `java.version` 21 | Java 21 é requisito, e `record` nos DTOs depende dele |
| Lombok + `annotationProcessorPaths` no compiler-plugin | nada | DTOs são `record` e a entidade tem acessores explícitos; o plugin volta à configuração padrão |
| `com.backend:backend`, pacote `com.backend.backend` | `com.taskmanager:task-manager`, pacote `com.taskmanager` | o pacote nomeia o domínio, não a camada onde o código roda |
| `BackendApplication` | `TaskManagerApplication` | nome da aplicação, não do seu papel na topologia |
| `<name/>`, `<description/>`, `<url/>`, `<licenses/>`, `<developers/>`, `<scm/>` vazios | removidos ou preenchidos | metadado vazio é ruído |
| `application.properties` | `application.yml` | a configuração tem estrutura aninhada (datasource, jpa, flyway) |

Outros impactos:

- Fixa decisões que todas as features seguintes herdam: `Instant` em UTC para
  auditoria, enum como texto, migration como única fonte do schema.
- Liga o gate mecânico do SDD ao build Java: `scripts/spec-tap.mjs` traduz os
  relatórios do Surefire para TAP, que é o formato que o motor de auditoria lê.
- Ativa na constituição os princípios P-002 (schema versionado) e P-003
  (segredos fora do código), que até aqui estavam na fila por falta de arquivos
  para verificar.
- Introduz o teste de regras de camada, que passa a ser a rede de segurança da
  arquitetura nas features seguintes.

## Dependências

- **Externas:** Docker e Docker Compose na máquina de quem executa.
- **Bloqueia:** 002-task-domain e todas as features seguintes.
- **Depende de:** nada — é a primeira feature.
- **Bibliotecas** (cada uma com a razão de existir):

| Dependência | Por que entra |
|---|---|
| `spring-boot-starter-webmvc` | expor a API REST |
| `spring-boot-starter-validation` | validação declarativa dos DTOs (features seguintes) |
| `springdoc-openapi-starter-webmvc-ui` | publicar o documento OpenAPI e o Swagger UI (AC-004) |
| `spring-boot-starter-data-jpa` | mapeamento objeto-relacional |
| `spring-boot-starter-flyway` + `flyway-database-postgresql` | migrations versionadas |
| `postgresql` (driver, escopo runtime) | acesso ao banco |
| `spring-boot-starter-actuator` | endpoint de saúde usado pelo healthcheck do compose (RNF-04) |
| `spring-boot-starter-*-test` (webmvc, data-jpa, flyway, actuator) | fatias de teste do Spring Boot 4: JUnit 5, AssertJ, MockMvc |
| `spring-boot-testcontainers` + `testcontainers-junit-jupiter` + `testcontainers-postgresql` | PostgreSQL real nos testes de integração (ASM-001) |
| `archunit-junit5` | transformar a regra de dependência entre camadas em teste (AC-005 a AC-007) |

O projeto usa **Spring Boot 4.1.1**, a versão estável corrente. A linha 3.x não
é mais oferecida pelo Spring Initializr nem recebe correções. O Boot 4 renomeou
alguns starters (`spring-boot-starter-webmvc` no lugar de `-web`,
`spring-boot-starter-flyway` no lugar da dependência avulsa) e quebrou o antigo
`spring-boot-starter-test` em fatias por tecnologia — os nomes acima já
refletem isso.

Não entram: Lombok (DTOs são `record` e a entidade tem acessores explícitos —
evita um processador de anotações e um plugin de IDE), MapStruct (mapear oito
campos não justifica gerar código), MySQL (o projeto é PostgreSQL) e H2
(ver ASM-001).

## Fora de escopo

- Qualquer endpoint de tarefa (`/api/tasks`) — isso é da 003 em diante.
- Entidade, enums e repositório do domínio — isso é da 002. Nesta feature os
  pacotes das camadas nascem vazios, e as regras de camada já os guardam.
- Qualquer tela ou componente Angular além do esqueleto gerado pelo CLI.
- Autenticação, autorização e perfis de usuário (ASM-003).
- Pipeline de CI, deploy e observabilidade além do endpoint de saúde.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-001 | Os testes de integração usam Testcontainers com PostgreSQL, não H2: as migrations são específicas de PostgreSQL (`timestamptz`, `bigserial`) e um teste contra H2 provaria um schema que não é o de produção. Custo aceito: `mvn test` passa a exigir Docker rodando. | confirmada | Confirmada: Testcontainers com PostgreSQL; Docker é pré-requisito dos testes |
| ASM-002 | Não existe máquina de estados de status: qualquer transição é permitida. Se o domínio exigir transições restritas, isso vira regra de negócio da 006. | confirmada | Confirmada: sem máquina de estados nesta versão |
| ASM-003 | A aplicação não tem autenticação nem multiusuário: todas as tarefas pertencem a um único espaço compartilhado. | confirmada | Confirmada: aplicação sem autenticação |
| ASM-004 | O bytecode entregue é sempre Java 21. A máquina de desenvolvimento tem apenas JRE, sem `javac`, então o build acontece dentro de container — o mesmo caminho que a imagem do backend usa. | confirmada | Confirmada: `release 21` verificado em build containerizado |
| ASM-005 | O esqueleto gerado pelo Initializr é ajustado ao alvo, não recriado: renomeação de coordenadas e pacote, troca do driver, remoção do Lombok e elevação para Java 21. O projeto está vazio, então o custo da renomeação é de dois arquivos. | confirmada | Confirmada: pacote base `com.taskmanager`, sem Lombok, PostgreSQL |
| ASM-006 | Enquanto os pacotes das camadas estiverem vazios, as regras de camada rodam em modo que tolera conjunto vazio; elas ganham efeito real conforme as classes chegam, sem precisar ser reescritas. | confirmada | Confirmada: regra escrita uma vez, na 001 |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-001 | Nenhuma. As decisões de arquitetura, fluxo SDD, camada de persistência, escopo de testes, formato de prova, uso de Lombok, nome do pacote base e verificação das camadas foram tomadas antes desta especificação e estão registradas no CLAUDE.md. | respondida | Registradas em CLAUDE.md, seção "Decisões arquiteturais" |
