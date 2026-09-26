# Spec: 004 — Listagem com filtros de título e situação

> feature: 004-list-tasks
> status: rascunho

<!--
  US-xxx = história de usuário · AC-xxx = critério de aceite
  ASM-xxx = suposição · Q-xxx = pergunta em aberto
  Todo critério de aceite vira um teste cujo título carrega @spec:AC-xxx.
-->

## Objetivo

Entregar `GET /api/tasks?title=&status=`: a lista de tarefas, filtrável por
trecho do título e por situação, da mais recente para a mais antiga. E fechar um
buraco que a 003 deixou: endereço errado hoje responde 500, como se o servidor
tivesse falhado.

## Contexto

A porta do domínio já sabe responder a esta pergunta. A `buscar(titulo, status)`
existe desde a 002 e está provada contra PostgreSQL real pela AC-012 — filtro
ausente não restringe, título é trecho sem distinção de maiúsculas, ordem
`createdAt` decrescente. Esta feature não inventa busca nenhuma: ela liga a
porta à borda HTTP e decide o que acontece nos casos que a porta não conhece,
porque são casos de protocolo — lista vazia, situação que não existe, endereço
que não existe.

O índice em `status` criado pela `V1` foi feito para este filtro. A busca por
título continua sem índice, pela razão que a Q-002 da 002 já registrou: é `LIKE`
com curinga à esquerda, e a decisão é esperar volume real.

**O segundo assunto da feature é uma dívida da 003, não escopo novo.** O
`@ExceptionHandler(Exception.class)` do `GlobalExceptionHandler` captura também
as exceções de roteamento do próprio Spring, então `GET /api/rota-que-nao-existe`
sai como 500 `INTERNAL_ERROR`. É errado em dois sentidos: o servidor não falhou,
e um 500 esconde de quem integra que o problema é o endereço. Consertar custa um
`@ExceptionHandler` no arquivo que a 003 já criou, e entra aqui porque é aqui
que a API passa a ter mais de uma rota de leitura — o erro fica muito mais
provável a partir de agora.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-008 — Encontrar tarefas por título e por situação

Como pessoa que usa o sistema, quero ver todas as minhas tarefas e estreitar a
lista por um trecho do título ou pela situação, para que eu encontre o que
preciso sem varrer a lista inteira.

#### AC-021 — A lista vem da mais recente para a mais antiga

- **Dado** três tarefas gravadas em momentos diferentes
- **Quando** a listagem é pedida sem filtro nenhum (`GET /api/tasks`)
- **Então** a resposta é 200 com as três tarefas, da mais recente para a mais
  antiga, cada uma no mesmo formato que a criação devolve

#### AC-022 — Os dois filtros funcionam juntos e separados

- **Dado** as tarefas "Escrever a spec" (concluída), "escrever o teste"
  (pendente) e "Revisar o PR" (pendente)
- **Quando** a listagem recebe o trecho `escrever`, depois a situação
  `PENDENTE`, depois os dois juntos, e depois um título em branco
- **Então** o trecho devolve as duas primeiras, a situação devolve as duas
  pendentes, a combinação devolve só "escrever o teste", e o título em branco é
  tratado como filtro ausente — devolve todas as três

#### AC-023 — Nada encontrado é lista vazia, não erro

- **Dado** um banco sem nenhuma tarefa que casa com o filtro
- **Quando** a listagem é pedida com esse filtro
- **Então** a resposta é 200 com uma lista vazia — nunca 404, porque a coleção
  existe e está vazia

#### AC-024 — Situação que não existe é recusada como requisição malformada

- **Dado** uma listagem pedida com a situação `URGENTE`, valor que o enum não
  tem
- **Quando** a listagem é pedida
- **Então** a resposta é 400 com `error` igual a `MALFORMED_REQUEST`, no mesmo
  contrato de erro das demais rotas

### US-009 — Erro de protocolo não parece falha do servidor

Como pessoa que integra com a API, quero que endereço inexistente e método não
suportado respondam com o status correto de protocolo, para que eu saiba que
errei a requisição em vez de suspeitar que o serviço caiu.

#### AC-025 — Rota inexistente responde 404 no contrato único de erro

- **Dado** um endereço que a API não publica (`/api/rota-que-nao-existe`)
- **Quando** ele é requisitado contra a aplicação de pé
- **Então** a resposta é 404 com `error` igual a `RESOURCE_NOT_FOUND`, `status`
  `404`, `timestamp` em UTC e `path` igual ao endereço pedido — nunca 500, e
  nunca a página de erro padrão do servidor

#### AC-026 — Método não suportado responde 405, e diz o que a rota aceita

- **Dado** uma rota que existe mas não aceita o método pedido (`DELETE
  /api/tasks`, enquanto a exclusão não existe)
- **Quando** ela é requisitada contra a aplicação de pé
- **Então** a resposta é 405 com `error` igual a `METHOD_NOT_ALLOWED`, no mesmo
  contrato de erro, e o header `Allow` lista os métodos que a rota aceita —
  nunca 500

## Requisitos não funcionais

- **RNF-16** — A porta do domínio não muda. A `buscar(titulo, status)` da 002
  atende a listagem inteira; a ASM-009 continua valendo — operação nova só entra
  quando uma feature precisar.
- **RNF-17** — Nenhuma dependência nova, nenhuma migration, nenhuma mudança de
  schema. O índice de `status` já existe desde a `V1`.
- **RNF-18** — A tradução de exceção em resposta continua inteira no
  `@RestControllerAdvice`: a rota de listagem não ganha `try/catch`.
- **RNF-19** — A listagem devolve um array puro, sem envelope e sem cabeçalho de
  total (ASM-016).

## Regras de negócio

Parâmetros de consulta, os dois opcionais:

| Parâmetro | Tipo | Comportamento |
|---|---|---|
| `title` | texto | trecho do título, sem distinção de maiúsculas; ausente, vazio ou só espaços não restringe |
| `status` | `TaskStatus` | igualdade exata; valor fora do enum é 400 |

- A ordem é `createdAt` decrescente, fixa (Q-003 da 002). Ordenação configurável
  vira requisito da feature que precisar dela.
- Sem paginação (D-8 do CLAUDE.md): a listagem devolve tudo que casa com o
  filtro.
- Título em branco é normalizado para ausente **no service**, não no controller:
  a borda HTTP entrega o que recebeu, e a regra de "em branco é o mesmo que não
  informado" é da camada de aplicação (ASM-017).
- Lista vazia é resultado legítimo, não erro (AC-023). O 404 continua reservado
  para a tarefa que não existe, pedida por identificador.

## Casos de erro

| Situação | Status | `error` |
|---|---|---|
| `status` fora do enum na consulta | 400 | `MALFORMED_REQUEST` (já mapeado na 003) |
| Rota inexistente | 404 | `RESOURCE_NOT_FOUND` (novo nesta feature) |
| Método não suportado na rota existente | 405 | `METHOD_NOT_ALLOWED` (novo nesta feature) |
| Filtro que não casa nada | 200 | nenhum erro: lista vazia |
| Qualquer outra | 500 | `INTERNAL_ERROR`, sem vazar stack trace |

## Impacto técnico

Nenhuma classe nova. Três arquivos da 003 são estendidos, e é o esperado: a
feature acrescenta uma rota a um controller que existe e um mapeamento a um
handler que existe.

| Arquivo | Origem | O que muda |
|---|---|---|
| `TaskService` | T-019 da 003 | ganha `listar(titulo, status)`, com a normalização do título em branco |
| `TaskController` | T-020 da 003 | ganha `@GetMapping` na raiz, com os dois `@RequestParam` opcionais |
| `GlobalExceptionHandler` | T-021 da 003 | ganha `NoResourceFoundException` para 404 `RESOURCE_NOT_FOUND` e `HttpRequestMethodNotSupportedException` para 405 `METHOD_NOT_ALLOWED` |
| `CLAUDE.md` | — | a tabela de erros ganha as duas linhas novas de protocolo |
| `README.md` | — | a listagem sai de "próxima feature" para implementada |

Decisões:

- **O `TaskMapper` não ganha método de lista.** Converter uma coleção é um
  `stream().map()` de uma linha no service; um método `paraRespostas` só
  existiria para esconder isso.
- **`RESOURCE_NOT_FOUND` é um código novo no contrato de erro**, diferente de
  `TASK_NOT_FOUND`: um diz que o endereço não existe, o outro que a tarefa não
  existe. Colapsar os dois faria o cliente confundir rota errada com tarefa
  apagada.
- **O 405 carrega o header `Allow`**, montado a partir dos métodos que a
  exceção do Spring já informa. Devolver 405 sem dizer o que a rota aceita
  obriga quem integra a adivinhar — e o dado está na mão de quem trata o erro.
- **As três classes de teste da 003 são estendidas**, em vez de nascerem
  paralelas: o `TaskControllerTest` já tem o service substituído e o
  `TaskApiIntegrationTest` já sobe a aplicação inteira. Criar
  `TaskListingControllerTest` ao lado duplicaria montagem para provar a mesma
  camada.

## Dependências

- **Depende de:** 002-task-domain (a porta `buscar`, provada pela AC-012) e
  003-create-task (service, controller, handler e o contrato de erro).
- **Bloqueia:** 008-frontend-tasks, cuja tela de listagem consome esta rota.
- **Externas:** Docker rodando, para os testes de integração.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- Paginação e ordenação configurável (D-8 e Q-003 da 002).
- Índice trigram para a busca por título (Q-002 da 002: só com volume medido).
- Substituição (`PUT`), troca de situação (`PATCH`) e exclusão (`DELETE`).
- Contagem total ou envelope de metadados na resposta (ASM-016).
- Filtro por prioridade ou por prazo: não é requisito hoje.
- Qualquer coisa de frontend — a interface é a 008, decisão reafirmada em
  26/09/2026.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-016 | A listagem devolve um array puro (`[{...}]`), sem envelope nem contagem total. Se a paginação entrar depois (D-8), o corpo muda de forma e isso é quebra de contrato para quem consome — assumida agora em troca de não carregar envelope que ninguém usa. | confirmada | Confirmada em 26/09/2026: array puro; envelope entra só se a paginação virar requisito |
| ASM-017 | A normalização de "título em branco é o mesmo que título ausente" fica no service, não no controller. A borda HTTP entrega o que recebeu; decidir o que um filtro vazio significa é regra de aplicação. | confirmada | Confirmada em 26/09/2026: normalização no service |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-006 | Método não suportado numa rota que existe (por exemplo `DELETE /api/tasks` antes da feature de exclusão) cai no mesmo `@ExceptionHandler(Exception.class)` e também vira 500 — é a mesma família de defeito que a AC-025 conserta. | respondida | Mapeado junto, na T-026: 405 `METHOD_NOT_ALLOWED` com header `Allow`, provado pela AC-026 |
