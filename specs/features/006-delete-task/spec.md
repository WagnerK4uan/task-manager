# Spec: 006 — Exclusão da tarefa

> feature: 006-delete-task
> status: em-implementacao

## Objetivo

Entregar a última rota do CRUD: `DELETE /api/tasks/{id}`, que remove a tarefa e
responde 204 sem corpo. Hoje o endereço existe e o método não — a resposta é 405
com o header `Allow`, exatamente como a AC-026 da 004 provou.

## Contexto

Esta é a primeira feature que **não cria nenhuma classe de produção**. A porta do
domínio já expõe `excluirPorId(Long)` desde a 002, e a ASM-009 registrou por quê:
a porta nasceu com as quatro operações que o CRUD das features 003 a 007 usaria,
e a exclusão é a última delas a ganhar chamador. O `TaskRepositoryAdapter` já
delega para `deleteById`, e a AC-011 já exercitou as quatro operações contra um
Postgres real.

A decisão de quem verifica a existência também já está tomada, e está escrita no
contrato da porta: *"Remove a linha; verificar se ela existia é papel da camada de
aplicação."* A 002 foi além e registrou a consequência em "Casos de erro":
excluir id inexistente "não encontra linha e nada acontece; virar 404 é decisão
da camada de aplicação". Esta feature é essa decisão.

**O comportamento do `deleteById` foi verificado, não suposto.** No Spring Data
JPA 4.0.4 o método é `findById(id).ifPresent(this::delete)`: id ausente é no-op
silencioso, sem `EmptyResultDataAccessException`. Isso importa em dois pontos —
o service não pode contar com uma exceção da porta para produzir o 404 (ele
carrega antes), e a corrida descrita na ASM-023 termina em dois 204, não em 500.

O contrato de erro não muda. `TaskNotFoundException` → 404 `TASK_NOT_FOUND`
desde a 003, id não numérico → 400 `MALFORMED_REQUEST` provado pela AC-020, e
método não aceito → 405 com `Allow` provado pela AC-026. Como na 005, nenhum
mapeamento novo entra no `GlobalExceptionHandler`.

Com esta feature o backend fecha: as seis rotas da tabela do CLAUDE.md passam a
existir, e o que resta do projeto é a interface (008).

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-012 — Excluir uma tarefa

Como pessoa que usa o sistema, quero remover uma tarefa que não preciso mais
fazer, para que a lista mostre só o trabalho que ainda é meu — sem que uma tarefa
cancelada tenha que ficar lá marcada como concluída.

#### AC-034 — A exclusão remove a tarefa e responde 204 sem corpo

- **Dado** duas tarefas gravadas
- **Quando** `DELETE /api/tasks/{id}` é pedido para a primeira
- **Então** a resposta é 204 com corpo vazio, uma consulta seguinte ao mesmo id é
  404 `TASK_NOT_FOUND`, e a segunda tarefa continua sendo devolvida pela listagem

#### AC-035 — Excluir tarefa que não existe é 404, no contrato único de erro

- **Dado** um identificador que não corresponde a nenhuma tarefa
- **Quando** a exclusão é pedida para ele
- **Então** a resposta é 404 com `error` igual a `TASK_NOT_FOUND`, `path` igual ao
  endereço pedido e `timestamp` em UTC — a mesma forma das demais rotas, e nada é
  removido

#### AC-036 — A segunda exclusão do mesmo id é 404, e o estado do servidor é o mesmo

- **Dado** uma tarefa gravada
- **Quando** `DELETE /api/tasks/{id}` é pedido duas vezes para ela
- **Então** a primeira resposta é 204 e a segunda é 404 `TASK_NOT_FOUND`, e depois
  das duas a tarefa continua ausente: o efeito no servidor é o mesmo das duas
  vezes, é só a resposta que difere (Q-008)

#### AC-037 — A exclusão é da tarefa, não da coleção

- **Dado** a rota do item e a rota da coleção
- **Quando** um método que a rota do item não aceita é pedido (`POST
  /api/tasks/{id}`), e depois `DELETE /api/tasks` na coleção
- **Então** as duas são 405 `METHOD_NOT_ALLOWED` no contrato único, o header
  `Allow` da rota do item passa a listar a exclusão junto com `GET` e `PUT`, e o
  da coleção continua sem ela — não existe exclusão em massa

## Requisitos não funcionais

- **RNF-24** — A porta do domínio não muda. `excluirPorId` existe desde a 002 e é
  exercitada pela AC-011; a ASM-009 continua valendo, e nenhuma operação nova
  entra na porta (ASM-022).
- **RNF-25** — Nenhum mapeamento novo no `GlobalExceptionHandler`: 404, 400 de id
  malformado e 405 com `Allow` já existem. A tradução de exceção em resposta
  continua inteira no `@RestControllerAdvice`, e a rota nova não ganha
  `try/catch`.
- **RNF-26** — Nenhuma dependência nova, nenhuma migration, nenhuma mudança de
  schema e nenhuma classe de produção nova. A exclusão é física: não há coluna de
  descarte nem filtro novo nas consultas existentes (ASM-021).
- **RNF-27** — A resposta 204 não tem corpo. Nem a tarefa excluída, nem um
  envelope de confirmação: quem pediu a exclusão já sabe o que pediu.

## Regras de negócio

- **A exclusão é física e definitiva.** A linha sai da tabela; não há lixeira,
  nem coluna de descarte, nem desfazer (ASM-021).
- **Quem decide o 404 é a camada de aplicação**, como o contrato da porta declara
  desde a 002: o service carrega a tarefa e só então manda excluir.
- **Id desconhecido é 404**, inclusive na segunda exclusão do mesmo id (Q-008). A
  idempotência do `DELETE` é sobre o efeito no servidor, não sobre o código de
  resposta — e o efeito é o mesmo nas duas chamadas.
- **Qualquer tarefa pode ser excluída**, em qualquer situação, inclusive
  `EM_ANDAMENTO` e `CONCLUIDA`. Máquina de estados não é requisito desta versão,
  como a ASM-002 da 001 já registrou.
- **Nada é excluído em cascata**, porque a tarefa não tem relação com outra
  entidade: uma tabela, uma linha.
- **Confirmação é da interface, não da API.** O backend não exige parâmetro de
  confirmação nem cabeçalho extra; perguntar "tem certeza?" é papel da 008.
- **A coleção não aceita exclusão.** Só a rota do item ganha o método, e é isso
  que mantém a AC-026 da 004 verdadeira.

## Casos de erro

| Situação | Status | `error` |
|---|---|---|
| id inexistente, ou segunda exclusão do mesmo id | 404 | `TASK_NOT_FOUND` (já mapeado na 003) |
| id não numérico na rota | 400 | `MALFORMED_REQUEST` (provado pela AC-020 da 003) |
| `DELETE` na coleção, ou método não aceito na rota do item | 405 | `METHOD_NOT_ALLOWED` com header `Allow` (já mapeado na 004) |
| endereço que não existe | 404 | `RESOURCE_NOT_FOUND` (provado pela AC-025 da 004) |
| qualquer outra | 500 | `INTERNAL_ERROR`, sem vazar stack trace |

Não há caso de erro de validação: a rota não tem corpo para validar.

## Impacto técnico

Nenhuma classe nova. Dois arquivos de produção ganham um método cada.

| Arquivo | Origem | O que muda |
|---|---|---|
| `TaskService` | T-024 da 004 | ganha `excluir(id)`: carrega pela porta e chama `excluirPorId` |
| `TaskController` | T-025 da 004 | ganha `@DeleteMapping("/{id}")`, `void`, com `@ResponseStatus(NO_CONTENT)` |
| `README.md` | — | a exclusão sai de "próxima feature" para implementada, e o bloco de estado atual para de dizer que ela falta |
| `004-list-tasks/spec.md` | — | a ressalva "enquanto a exclusão não existe" da AC-026 deixa de ser verdade (Q-009) |

Decisões:

- **`carregar(id)` + `excluirPorId(id)`, e não um `existe(id)` na porta.** O
  service reaproveita o `carregar` privado que a 003 criou e que já traduz
  ausência em `TaskNotFoundException` — a decisão do 404 não se repete pela
  quarta vez. O custo é materializar a entidade só para descartá-la, e duas
  buscas por chave primária (o `deleteById` faz a sua própria). Um `existe(id)`
  na porta economizaria isso e custaria uma operação nova contra a ASM-009, por
  um ganho que não existe em tabela deste tamanho (ASM-022).
- **`void` com `@ResponseStatus(HttpStatus.NO_CONTENT)`.** `ResponseEntity<Void>`
  diria o mesmo com mais cerimônia e abriria espaço para o controller escolher
  status — o que ele não precisa fazer. O método `void` também deixa explícito
  que não há corpo a montar.
- **Sem `@Transactional`, como nas demais operações.** As duas chamadas da porta
  têm transações próprias; a janela entre elas está registrada na ASM-023.
- **O service devolve `void`, não a tarefa excluída.** Devolver o que foi apagado
  seria dado que o chamador não pediu, e a RNF-27 já fecha a resposta sem corpo.

## Dependências

- **Depende de:** 002-task-domain (a operação `excluirPorId` da porta e o
  adaptador, provados pela AC-011), 003-create-task (o `TaskNotFoundException`,
  o `carregar` privado do service e o contrato de erro) e 004-list-tasks (o
  service e o controller onde o método entra, e o 405 com `Allow`).
- **Bloqueia:** 008-frontend-tasks — o botão de excluir da listagem consome esta
  rota.
- **Externas:** Docker rodando, para os testes de integração.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- **Exclusão em massa** (`DELETE /api/tasks`, com ou sem filtro): não é
  requisito, e a coleção continua respondendo 405 — o que a AC-037 prova.
- **Lixeira, soft delete e desfazer** (ASM-021): exigiriam coluna nova, migration,
  filtro em toda consulta existente e uma rota de restauração.
- **Histórico ou trilha de auditoria da exclusão**: nada registra quem excluiu o
  quê, porque a tarefa não tem dono (ASM-003 da 001).
- **Bloqueio otimista e 409**: reafirmado da 005 (ASM-019), agora para a corrida
  entre duas exclusões (ASM-023).
- **Restrição de exclusão por situação da tarefa**: qualquer situação pode ser
  excluída.
- Qualquer coisa de frontend, inclusive a confirmação antes de excluir — a
  interface é a 008.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-021 | A exclusão é física e definitiva: a linha sai da tabela e não há como desfazer. Soft delete daria o desfazer ao custo de uma coluna, uma migration, um filtro em toda consulta já escrita (incluindo a JPQL da 004) e uma rota de restauração — assumido agora porque nenhum requisito pede recuperar tarefa excluída. O preço é que exclusão por engano é perda de dado. | confirmada | Confirmada em 27/09/2026: exclusão física e definitiva; lixeira, soft delete e desfazer ficam fora desta versão |
| ASM-022 | A porta não ganha `existe(id)`: o service materializa a entidade pelo `carregar` só para decidir o 404, e o `deleteById` faz a sua própria busca por chave primária logo depois. São duas leituras por chave primária onde uma bastaria, em troca de manter a porta nas quatro operações da ASM-009 e de reaproveitar a tradução do 404 que já existe. | confirmada | Confirmada em 27/09/2026: a porta fica nas quatro operações; o service carrega para decidir o 404 |
| ASM-023 | Sem controle de concorrência e sem transação única: duas exclusões simultâneas do mesmo id podem ambas passar pelo `carregar` antes de qualquer remoção, e as duas respondem 204 — a segunda `excluirPorId` não encontra linha e, como o `deleteById` do Spring Data 4.0.4 é `findById(...).ifPresent(...)`, nada acontece e nada é lançado. Duas respostas de sucesso para uma exclusão só. É a mesma ausência de controle já assumida na ASM-019 da 005, e não há requisito de concorrência. | confirmada | Confirmada em 27/09/2026: sem controle de concorrência; `@Version` e 409 entram só se houver requisito |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-008 | A segunda exclusão do mesmo id responde 404 ou 204? O argumento do 204 é a idempotência do `DELETE`: o estado final é o mesmo, e o cliente que perdeu a resposta da primeira chamada pode repetir sem ver erro. Recomendação: **404**. A idempotência do HTTP é sobre o efeito no servidor, não sobre o código de resposta — e o efeito é idêntico nas duas chamadas de qualquer forma. Já o 204 para id inexistente faria `/api/tasks/{id}` responder de um jeito no `DELETE` e de outro no `GET`, `PUT` e `PATCH`, que são 404 desde a 003, e o cliente perderia a diferença entre "apaguei agora" e "esse id nunca existiu". O preço é que a interface da 008 precisa tratar 404 na exclusão como "já não está lá", e não como falha a mostrar. Se a resposta for 204, a AC-036 se inverte antes da implementação. | respondida | Respondida em 27/09/2026: a segunda exclusão do mesmo id é 404 `TASK_NOT_FOUND` — a idempotência é do efeito, não do status; a AC-036 fica como está |
| Q-009 | A AC-026 da 004 tem a ressalva "`DELETE /api/tasks`, enquanto a exclusão não existe" no seu **Dado**. Depois desta feature a exclusão existe, e o critério continua verdadeiro e provado — a coleção segue sem aceitar `DELETE` —, mas a justificativa entre parênteses deixa de descrever o projeto. Recomendação: corrigir a ressalva na 004 na mesma tarefa que atualiza o README, dizendo que a coleção não aceita exclusão porque a exclusão é do item. Não é reabrir critério auditado: o Dado, o Quando e o Então continuam os mesmos, nenhum teste muda e a prova gravada não é afetada (`specs/**` está em `ignoreGlobs`). A alternativa é deixar como está e conviver com uma linha que envelheceu — mais barata, e contra o princípio de que a spec continua verdadeira. | respondida | Respondida em 27/09/2026: a ressalva da AC-026 da 004 é corrigida na T-038, junto com o README; o critério e a prova não mudam |
