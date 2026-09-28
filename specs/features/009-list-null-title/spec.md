# Spec: 009 — Listagem sem filtro de título

> feature: 009-list-null-title
> status: auditada

## Objetivo

Fazer `GET /api/tasks` responder 200 quando não há filtro de título — hoje
responde 500 numa aplicação recém-subida — e provar isso com um teste que a
ordem de execução não consiga esconder.

## Contexto

A consulta da 004 é uma só, com os dois filtros opcionais:

```
where (:titulo is null or lower(t.title) like lower(concat('%', :titulo, '%')))
```

Quando `:titulo` é nulo, o Hibernate não tem de onde inferir o tipo do
parâmetro e o PostgreSQL resolve o `||` como `bytea`:

```
ERROR: function lower(bytea) does not exist
STATEMENT: ... where ($1 is null or lower(t1_0.title) like lower(('%'||$2||'%')) escape '')
```

O plano fica no cache de planos do Hibernate, por consulta, e **a primeira
execução decide**: backend recém-iniciado que recebe uma listagem sem filtro
responde 500 em toda listagem sem filtro dali em diante; se antes dela passar
uma chamada com `?title=`, o parâmetro é tipado e a listagem sem filtro passa a
funcionar. As duas ordens foram reproduzidas contra o compose, com reinício do
backend no meio.

**Por que 44 testes verdes não pegaram isso.** As classes de integração
estendem a mesma base e compartilham um contexto Spring — logo, um cache de
planos. Alguma classe executa a consulta com um título real antes de
`buscar(null, null)`, o parâmetro fica tipado, e o caso nulo passa de carona. A
suíte está verde por acidente de ordem, não por a consulta estar correta.

**Quem encontrou.** O harness de ponta a ponta da 007, na primeira execução: o
critério AC-039 pede `GET /api/tasks` pelo endereço do frontend depois de criar
uma tarefa, e recebeu 500. É o primeiro defeito que o gate achou sozinho, e é
exatamente a requisição que a tela da 008 faz ao abrir.

## Requisitos funcionais

Expressos como história e critério de aceite abaixo.

## Histórias

### US-016 — Abrir a listagem sem informar filtro

Como pessoa que usa a aplicação, quero abrir a lista de tarefas sem informar
filtro nenhum e receber as tarefas, para que a primeira carga da tela não seja
um erro do servidor.

#### AC-047 — A listagem sem filtro de título responde 200 na primeira execução da consulta

- **Dado** uma fábrica de sessões nova, em que essa consulta nunca foi
  executada — nenhuma execução anterior pode ter tipado o parâmetro
- **Quando** a listagem é pedida sem filtro nenhum, e também quando é pedida
  apenas com situação, sem título
- **Então** as duas respondem com as tarefas gravadas, na ordem da mais recente
  para a mais antiga — nunca com erro de banco

## Requisitos não funcionais

- **RNF-32** — A correção é uma linha de JPQL. Nenhum DTO, rota, migration,
  entidade ou componente entra: o contrato da API não muda, só o tipo do
  parâmetro que vai ao banco.
- **RNF-33** — O teste de regressão não pode depender de ordem. Ele roda em
  contexto Spring novo, descartado antes da classe, para que a fábrica de sessões
  seja nova e a primeira execução da consulta seja a dele — é o que reproduz o
  defeito e o que impede que a suíte volte a esconder a próxima ocorrência
  (ASM-033).
- **RNF-34** — Nenhum `try/catch` novo e nenhuma mudança no
  `GlobalExceptionHandler`: o defeito é de consulta, não de tradução de erro.
  Transformar o 500 em outra resposta seria maquiar o sintoma.

## Regras de negócio

A semântica da listagem não muda — ela é a da 004 e continua valendo:

- filtro de título ausente, nulo ou em branco não restringe nada;
- filtro de título presente é trecho, indiferente a maiúsculas;
- situação é igualdade exata;
- a ordem é do `createdAt` mais recente para o mais antigo.

A única mudança é dar tipo ao parâmetro nulo antes de ele chegar ao banco.

## Casos de erro

| Situação | Comportamento |
|---|---|
| `GET /api/tasks` sem nenhum parâmetro | 200 com todas as tarefas (hoje: 500 `INTERNAL_ERROR`) |
| `GET /api/tasks?status=PENDENTE`, sem título | 200 com as tarefas daquela situação (hoje: 500) |
| `GET /api/tasks?title=` | 200 — filtro em branco não restringe, como na 004 |
| Situação fora do enum | 400 `MALFORMED_REQUEST`, como na 004 — inalterado |

## Impacto técnico

Uma linha de consulta e uma classe de teste.

| Arquivo | Origem | O que muda |
|---|---|---|
| `infrastructure/persistence/TaskJpaRepository.java` | T-019 da 004 | `cast(:titulo as string)` dentro do `concat` |
| `test/.../infrastructure/TaskListingWithoutFilterTest.java` | novo | reproduz o defeito e prova a AC-047, em contexto descartado antes da classe |

Decisões:

- **`cast(:titulo as string)` na JPQL.** Dá tipo ao parâmetro onde o problema
  está, mantém uma consulta só e não move regra de consulta para o service. As
  duas alternativas foram recusadas: **normalizar para string vazia no service**
  transformaria "sem filtro" num `like '%%'` sobre a tabela e mudaria a regra de
  lugar; **uma consulta derivada por combinação de filtro** trocaria um caminho
  por quatro, que é justamente o que a JPQL única evita.
- **Contexto novo no teste, não cache de planos desligado.** A primeira
  tentativa foi `hibernate.query.plan_cache_max_size=0`, e o Hibernate recusa:
  `Maximum capacity has to be at least twice the concurrencyLevel` — o contexto
  nem sobe, e o teste falha por erro de infraestrutura, que é indistinguível de
  prova para quem lê só o vermelho. A garantia vem de descartar o contexto antes
  da classe: fábrica de sessões nova, cache de planos vazio, primeira execução da
  consulta é a do teste. Desligar o cache na aplicação nunca foi opção — seria
  pagar recompilação de HQL em produção para esconder o defeito em vez de
  corrigi-lo.
- **Teste antes da correção.** A ordem das features anteriores é implementação e
  depois prova; aqui ela se inverte porque, num defeito, um teste que não falhou
  antes não provou que reproduz.
- **Nenhuma outra consulta revisada.** É a única consulta do projeto com
  parâmetro opcional dentro de função — as demais são derivadas do Spring Data
  ou por chave primária.

## Dependências

- **Depende de:** 004-list-tasks (a consulta com filtros) e 002-task-domain (a
  porta e a entidade).
- **Bloqueia:** 007-e2e-proof, cujo critério AC-039 só passa com a listagem
  consertada, e 008-frontend-tasks, que abre a tela pedindo a lista sem filtro.
- **Externas:** Docker rodando, para o Testcontainers e para o compose.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- **Paginação** e qualquer mudança no contrato da listagem — continua o D-8.
- **Specification/Criteria API**: a JPQL segue legível com um `cast`, e trocar de
  mecanismo de consulta por causa de um parâmetro sem tipo seria desproporcional.
- **Filtro novo** (prioridade, prazo): não é requisito desta correção.
- **Índice em `lower(title)`**: a busca por trecho já é varredura e não há
  volume que justifique — entra com medição, não por antecipação.
- **Revisar o cache de planos da aplicação** ou fixar tamanho dele: a aplicação
  fica como está.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-032 | O `cast(:titulo as string)` do HQL chega ao PostgreSQL como `cast(? as varchar)` e não muda o plano de execução da busca por trecho — que já é varredura, porque não existe índice sobre `lower(title)`. O preço é uma conversão explícita na consulta; o ganho é o parâmetro nulo deixar de ser `bytea`. | confirmada | Confirmada em 27/09/2026: correção aprovada pelo mantenedor com o `cast` na JPQL |
| ASM-033 | O teste de regressão descarta o contexto Spring antes da classe, para ter fábrica de sessões nova. O custo é um contexto extra na suíte — o container do PostgreSQL continua sendo o mesmo, compartilhado pela classe base. Sem isso, o teste voltaria a depender da ordem das outras classes, que é a causa de o defeito ter passado. | confirmada | Confirmada em 27/09/2026: contexto descartado antes da classe; a tentativa de desligar o cache de planos foi recusada pelo Hibernate e está registrada em "Impacto técnico" |

## Perguntas em aberto

Nenhuma. A causa foi reproduzida nas duas ordens de execução e a correção está
decidida; o que resta é implementar e provar.
