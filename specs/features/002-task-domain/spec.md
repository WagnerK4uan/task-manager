# Spec: 002 — Domínio da tarefa e porta de persistência

> feature: 002-task-domain
> status: auditada

<!--
  US-xxx = história de usuário · AC-xxx = critério de aceite
  ASM-xxx = suposição · Q-xxx = pergunta em aberto
  Todo critério de aceite vira um teste cujo título carrega @spec:AC-xxx.
-->

## Objetivo

Dar ao sistema o modelo de tarefa e o caminho até o banco: a entidade `Task`
mapeada na tabela que a 001 criou, os enums de status e prioridade, e a porta
de persistência declarada no domínio com o adaptador que a implementa — sem
nenhum endpoint, DTO ou regra de aplicação.

## Contexto

A 001 deixou o ambiente de pé: a tabela `tasks` existe por migration, o
Hibernate está em `validate` e as regras de camada são verificadas a cada
build. Só que os pacotes do domínio estão vazios: hoje o backend sobe, conecta
e não sabe o que é uma tarefa.

Esta feature preenche esse vazio pela parte de dentro. É deliberadamente a
menor fatia que ainda é verificável de ponta a ponta: com `ddl-auto: validate`,
a existência da entidade deixa de ser detalhe de código e passa a ser afirmação
sobre o schema — se o mapeamento divergir de uma coluna, a aplicação não sobe.
É a primeira vez que a AC-003 da 001 tem algo real para validar.

Também é aqui que a ASM-006 da 001 se cumpre: as regras ArchUnit, escritas
para pacotes vazios com `allowEmptyShould(true)`, passam a ter classes de
verdade para guardar.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-004 — Modelo de tarefa fiel ao banco

Como pessoa que mantém o sistema, quero que o modelo em Java e a tabela no
banco sejam a mesma coisa, para que uma divergência apareça na inicialização e
não como dado corrompido meses depois.

#### AC-008 — A tarefa gravada volta íntegra do banco

- **Dado** o contexto de persistência inicializado com `ddl-auto: validate`
  contra o schema das migrations
- **Quando** uma tarefa com os oito campos preenchidos é gravada e lida de novo
- **Então** todos os campos voltam com o mesmo valor, e o `id` é gerado pelo
  banco — o contexto não sobe se o mapeamento divergir de qualquer coluna

#### AC-009 — Status e prioridade são gravados como texto

- **Dado** uma tarefa com status `EM_ANDAMENTO` e prioridade `ALTA`
- **Quando** a linha é lida diretamente por SQL, sem passar pelo ORM
- **Então** as colunas contêm os textos `EM_ANDAMENTO` e `ALTA`, nunca a posição
  ordinal da constante

#### AC-010 — As datas de auditoria são preenchidas sozinhas

- **Dado** uma tarefa nova, sem `createdAt` nem `updatedAt` informados
- **Quando** ela é gravada e depois alterada
- **Então** as duas datas são preenchidas na criação, o `updatedAt` avança na
  alteração e o `createdAt` permanece o da criação

### US-005 — Porta de persistência pertencente ao domínio

Como pessoa que vai implementar os casos de uso, quero falar com o banco por
uma interface do próprio domínio, para que o service seja testável sem subir
Spring e para que trocar a tecnologia de persistência não alcance o centro da
aplicação.

#### AC-011 — As quatro operações funcionam através da porta

- **Dado** um PostgreSQL real com o schema das migrations
- **Quando** as operações de gravar, buscar por id, buscar com filtros e
  excluir são chamadas **pela interface declarada no domínio**
- **Então** todas se comportam como descrito em "Regras de negócio", sem que o
  chamador conheça Spring Data

#### AC-012 — A busca por título é parcial e indiferente a maiúsculas

- **Dado** as tarefas "Escrever a spec", "escrever o teste" e "Revisar o PR"
- **Quando** a busca recebe o trecho `escrever`
- **Então** as duas primeiras são devolvidas, da mais recente para a mais
  antiga; combinada com um status, a busca devolve só as que atendem aos dois
  filtros; sem filtro nenhum, devolve todas

#### AC-013 — Buscar um id inexistente é resultado vazio, não erro

- **Dado** um banco sem a tarefa de id `999`
- **Quando** a busca por esse id é feita pela porta
- **Então** o resultado é vazio (`Optional.empty()`), sem exceção e sem `null`
  — traduzir isso em 404 é papel da camada de aplicação, não da persistência

## Requisitos não funcionais

- **RNF-07** — O domínio continua sem dependência de Spring: a porta é uma
  interface Java, e Spring Data só aparece em `infrastructure`.
- **RNF-08** — Esta feature não altera o schema. Se o mapeamento exigir mudança
  de coluna, a mudança vira uma migration nova — a `V1` já aplicada nunca é
  editada.
- **RNF-09** — A consulta de busca é uma JPQL legível. Specification e Criteria
  API ficam fora enquanto os filtros forem dois.
- **RNF-10** — A regra de camada que guarda o domínio passa a enxergar também o
  Hibernate, hoje invisível para ela (ver "Impacto técnico").

## Regras de negócio

Valores dos enums, persistidos como texto:

| Enum | Valores |
|---|---|
| `TaskStatus` | `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA` |
| `TaskPriority` | `BAIXA`, `MEDIA`, `ALTA` |

Campos da tarefa — os tipos e a nulidade são os fixados pela 001:

| Campo | Obrigatório | Observação |
|---|---|---|
| `title` | sim | até 120 caracteres |
| `description` | não | até 2000 caracteres |
| `status` | sim | sem valor padrão na entidade |
| `priority` | sim | sem valor padrão na entidade |
| `dueDate` | não | data civil, sem fuso |
| `createdAt` / `updatedAt` | sim | preenchidos pela persistência, nunca pelo chamador |

Contrato das operações da porta:

| Operação | Comportamento |
|---|---|
| gravar | insere quando não há id e atualiza quando há; devolve a tarefa com id e datas preenchidos |
| buscar por id | devolve a tarefa ou vazio |
| buscar com filtros | título é trecho, sem distinção de maiúsculas; status é igualdade exata; filtro ausente não restringe; ordem padrão é `createdAt` decrescente |
| excluir por id | remove a linha; quem verifica a existência antes é a camada de aplicação |

- Não existe máquina de estados: qualquer transição de status é permitida
  (ASM-002 da 001, reafirmada aqui).
- A tarefa não tem dono nem escopo de usuário (ASM-003 da 001).

## Casos de erro

| Situação | Comportamento esperado |
|---|---|
| Gravar tarefa sem título, status ou prioridade | O banco rejeita pela restrição `NOT NULL` e a transação falha; a mensagem amigável por campo é da 003, com Bean Validation nos DTOs |
| Título acima de 120 caracteres | O banco rejeita pelo tamanho da coluna |
| Coluna com valor de enum desconhecido no código | O Hibernate falha ao converter a linha: um valor novo exige o deploy do enum antes do dado |
| Buscar id inexistente | Resultado vazio, sem exceção (AC-013) |
| Excluir id inexistente | A exclusão não encontra linha e nada acontece; virar 404 é decisão da camada de aplicação |
| Mapeamento divergente do schema | O Hibernate em `validate` aborta a inicialização apontando a coluna (AC-003 da 001) |

## Impacto técnico

Classes criadas, todas no pacote base `com.taskmanager`:

| Classe | Pacote | Papel |
|---|---|---|
| `TaskStatus`, `TaskPriority` | `domain.enums` | valores persistidos como texto |
| `Task` | `domain.entity` | entidade JPA, com acessores explícitos |
| `TaskRepository` | `domain.repository` | a porta: interface Java, sem Spring |
| `TaskJpaRepository` | `infrastructure.persistence` | Spring Data, com a JPQL da busca |
| `TaskRepositoryAdapter` | `infrastructure.persistence` | implementa a porta delegando ao Spring Data |

Decisões de mapeamento:

- Sem Lombok: a entidade tem construtor de negócio, construtor protegido sem
  argumentos exigido pelo JPA e acessores escritos à mão. `id`, `createdAt` e
  `updatedAt` não têm setter — quem os define é a persistência.
- `@Enumerated(EnumType.STRING)` nos dois enums.
- `@CreationTimestamp` e `@UpdateTimestamp` para as datas de auditoria, como o
  CLAUDE.md determina.

**A regra de camada precisa ser ajustada, e ela fica mais estrita, não menos.**
As duas anotações de timestamp são do Hibernate (`org.hibernate.annotations`),
não de `jakarta.persistence`. A AC-005 da 001 fala em tolerar apenas
`jakarta.persistence`, mas a regra escrita no `LayerDependencyTest` só proíbe
`org.springframework..` e `jakarta..` — `org.hibernate..` passa inteiro,
hoje, sem ninguém notar. Esta feature fecha o buraco: a regra passa a proibir
`org.hibernate..` com exceção nominal de `org.hibernate.annotations..`, que é
mapeamento pela mesma razão que `jakarta.persistence` é. O resultado é uma
regra que cobre mais do que cobria; nenhum princípio é afrouxado.

**A base de teste da 001 serve mais de uma classe pela primeira vez.** O
`PostgresIntegrationTest` era package-private e declarava o container com
`@Container`: o ciclo de vida do JUnit o parava ao fim de cada classe, e a
classe seguinte reaproveitava o contexto Spring em cache apontando para uma
porta que já não existia. Esta feature torna a base pública — as classes de
teste vivem em subpacotes — e passa a um container único, iniciado uma vez e
vivo enquanto a JVM de teste viver. É o que a ASM-011 já assumia; a 001 tinha
uma classe só, então a limitação não tinha como aparecer lá.

Outros impactos:

- A ASM-006 da 001 se cumpre: as regras ArchUnit deixam de rodar sobre pacotes
  vazios.
- A AC-003 da 001 (`validate`) passa a ter mapeamento real para conferir.
- Nenhuma dependência nova no `pom.xml`.
- Nenhuma migration nova.

## Dependências

- **Depende de:** 001-project-setup — schema, perfis, Testcontainers e as
  regras de camada.
- **Bloqueia:** 003-create-task e todo o restante do CRUD, que falam com o
  banco por esta porta.
- **Externas:** Docker rodando, para os testes de integração.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- Endpoints, DTOs, mapper e service — a partir da 003.
- `TaskNotFoundException`: nasce junto com quem a lança, na 003 (ASM-008).
- Validação declarativa de entrada: é Bean Validation nos DTOs, na 003. Aqui
  quem recusa dado inválido é a restrição do banco.
- Paginação e ordenação configurável na busca (D-8 do CLAUDE.md).
- Índice para a busca por título (Q-002).
- Máquina de estados de status (ASM-002 da 001).
- Qualquer coisa de frontend.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-007 | Os valores dos enums ficam em português (`PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `BAIXA`, `MEDIA`, `ALTA`), embora os identificadores do código sejam em inglês. O payload fica bilíngue — chaves e códigos de erro em inglês, valores em português —, em troca de casar com a interface em pt-BR e com os exemplos que o README já publica. | confirmada | Decisão do mantenedor em 26/09/2026 |
| ASM-008 | A exceção de domínio não entra aqui: uma classe que ninguém lança é código morto, e o audit a acusaria como órfã. Ela nasce na 003, junto com o service que a lança e o handler que a traduz. | confirmada | Confirmada: exceção fica para a 003 |
| ASM-009 | A porta nasce com as quatro operações que o CRUD das features 003 a 007 vai usar, e só com elas. Se uma feature precisar de outra, ela entra naquela feature — não por antecipação. | confirmada | Confirmada: porta com gravar, buscar por id, buscar com filtros e excluir por id |
| ASM-010 | A entidade não tem valor padrão de status nem de prioridade. Quem decide o que é uma tarefa recém-criada é o DTO de criação, na 003; a entidade exige os dois porque o banco exige. | confirmada | Confirmada: os dois campos são argumentos do construtor de negócio, sem padrão |
| ASM-011 | Os testes desta feature reaproveitam o `PostgresIntegrationTest` da 001 — mesmo container, mesmas migrations. Nenhuma fatia de teste nova é introduzida. | confirmada | Confirmada: mesma base, nenhuma fatia nova; a base virou pública e de container único (ver "Impacto técnico") |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-002 | A busca por título é `LIKE` com curinga à esquerda, que um índice B-tree não atende — a migration da 001 já registra isso. A partir de que volume vale um índice trigram (`pg_trgm`)? | respondida | Sem índice agora: o `pg_trgm` entra por migration quando houver volume medido que o justifique, não por antecipação |
| Q-003 | A ordem padrão da busca é `createdAt` decrescente, escolhida aqui para que a listagem não dependa da ordem física das linhas. Se a interface pedir outra (vencimento mais próximo, prioridade), isso vira requisito da 004 ou da 008 — e a porta ganha o parâmetro lá. | respondida | `createdAt` decrescente fixo na JPQL; ordenação configurável vira requisito da 004 ou da 008 |
