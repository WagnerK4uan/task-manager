# Spec: 005 — Substituição e troca de situação da tarefa

> feature: 005-update-task
> status: auditada

## Objetivo

Entregar as duas rotas de escrita que faltam sobre uma tarefa existente:
`PUT /api/tasks/{id}`, que substitui a tarefa inteira, e
`PATCH /api/tasks/{id}/status`, que troca só a situação. Hoje as duas respondem
405 — o endereço existe, o método não.

## Contexto

A porta do domínio já sabe gravar alteração: a `gravar(Task)` da 002 está
documentada como "insere quando não há id e atualiza quando há", e a entidade
carregada pela `buscarPorId` volta com id preenchido. A operação nova, portanto,
não é de persistência; é de aplicação — carregar, aplicar os campos novos,
gravar.

**A entidade também já sabe se alterar.** A `Task` da T-012 tem setter para os
cinco campos mutáveis (`title`, `description`, `status`, `priority`, `dueDate`) e
para nenhum dos três que são do servidor: `id`, `createdAt` e `updatedAt` só têm
acessor de leitura. O mapeamento fecha a regra do lado do banco — `created_at` é
`updatable = false` e `updated_at` é preenchido pelo `@UpdateTimestamp`. Esta
feature não acrescenta campo, método nem coluna: ela usa o que a 002 já provou.

O contrato de erro também está pronto. `TaskNotFoundException` → 404
`TASK_NOT_FOUND` desde a 003, `MethodArgumentNotValidException` → 400
`VALIDATION_ERROR` com `fields`, e corpo ilegível ou enum fora do conjunto → 400
`MALFORMED_REQUEST`. Nenhum mapeamento novo é necessário, e é a primeira feature
de escrita em que isso acontece.

A diferença entre as duas rotas é a que o CLAUDE.md já registra: `PUT` substitui
a tarefa inteira, inclusive a situação, porque o formulário de edição tem o
campo; `PATCH /status` existe para a troca rápida a partir da listagem, que é a
única modificação parcial que a interface precisa. Não é redundância — é
substituição contra modificação parcial.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-010 — Editar uma tarefa existente

Como pessoa que usa o sistema, quero corrigir qualquer campo de uma tarefa que
já criei, para que a lista continue refletindo o que eu preciso fazer sem que eu
tenha que apagar e recriar a tarefa.

#### AC-027 — A substituição troca os cinco campos e preserva a origem da tarefa

- **Dado** uma tarefa gravada
- **Quando** ela é substituída por `PUT /api/tasks/{id}` com os cinco campos
  diferentes dos gravados
- **Então** a resposta é 200 com o mesmo `id` e os valores novos, e uma consulta
  seguinte devolve os mesmos valores: `createdAt` é o mesmo de antes e
  `updatedAt` é posterior ao anterior

#### AC-028 — Campo obrigatório inválido é recusado campo a campo, e nada é gravado

- **Dado** uma substituição com título em branco, situação nula e prioridade nula
- **Quando** ela é enviada
- **Então** a resposta é 400 com `error` igual a `VALIDATION_ERROR` e um par
  `field`/`message` para cada campo recusado, e a camada de aplicação não é
  chamada — a tarefa gravada continua como estava

#### AC-029 — Prazo no passado não impede a edição

- **Dado** uma tarefa cujo prazo já venceu
- **Quando** ela é substituída mantendo esse prazo e mudando só o título
- **Então** a resposta é 200 com o título novo e o prazo vencido intacto — a
  regra `@FutureOrPresent` da criação não se aplica à edição (Q-007)

#### AC-030 — Substituir tarefa que não existe é 404, e não cria nada

- **Dado** um identificador que não corresponde a nenhuma tarefa
- **Quando** a substituição é pedida para ele
- **Então** a resposta é 404 com `error` igual a `TASK_NOT_FOUND` e nenhuma
  tarefa é criada — `PUT` não faz upsert, porque quem gera o identificador é o
  banco

### US-011 — Trocar a situação a partir da listagem

Como pessoa que usa o sistema, quero mudar a situação de uma tarefa sem reenviar
os outros campos, para que marcar algo como concluído a partir da lista seja uma
ação só.

#### AC-031 — A troca de situação muda a situação e mais nada

- **Dado** uma tarefa pendente, com descrição, prioridade e prazo preenchidos
- **Quando** `PATCH /api/tasks/{id}/status` recebe `{"status": "CONCLUIDA"}`
- **Então** a resposta é 200 com a situação nova e com título, descrição,
  prioridade, prazo e `createdAt` idênticos aos de antes

#### AC-032 — Situação ausente ou fora do enum é recusada

- **Dado** uma troca de situação com o corpo `{"status": null}`, e depois com o
  corpo `{"status": "URGENTE"}`
- **Quando** cada uma é enviada
- **Então** a primeira é 400 `VALIDATION_ERROR` com o par `field`/`message` do
  campo `status`, a segunda é 400 `MALFORMED_REQUEST`, e nenhuma das duas altera
  a tarefa

#### AC-033 — Trocar a situação de tarefa que não existe é 404

- **Dado** um identificador que não corresponde a nenhuma tarefa
- **Quando** a troca de situação é pedida para ele
- **Então** a resposta é 404 com `error` igual a `TASK_NOT_FOUND`, no mesmo
  contrato de erro das demais rotas

## Requisitos não funcionais

- **RNF-20** — A porta do domínio não muda. A `gravar(Task)` da 002 já atualiza
  quando o id está preenchido; a ASM-009 continua valendo — operação nova só
  entra quando uma feature precisar.
- **RNF-21** — Nenhuma dependência nova, nenhuma migration, nenhuma mudança de
  schema. `created_at` já é `updatable = false` e `updated_at` já é do
  `@UpdateTimestamp`.
- **RNF-22** — Nenhum mapeamento novo no `GlobalExceptionHandler`: 404, 400 de
  validação e 400 de corpo malformado já existem desde a 003. A tradução de
  exceção em resposta continua inteira no `@RestControllerAdvice`, e as duas
  rotas novas não ganham `try/catch`.
- **RNF-23** — A entidade não ganha campo nem método. Os setters dos cinco campos
  mutáveis existem desde a T-012, e `id`, `createdAt` e `updatedAt` continuam sem
  setter.

## Regras de negócio

- **`PUT` é substituição, não remendo.** Os cinco campos do corpo passam a ser os
  campos da tarefa. Campo opcional omitido (`description`, `dueDate`) apaga o
  valor gravado, porque o cliente declarou a tarefa inteira (ASM-018).
- **`PUT` não cria.** Identificador é `bigserial`: o cliente não tem como nomear
  o endereço de uma tarefa que ainda não existe, então id desconhecido é 404 e
  não um insert.
- **`PATCH /status` muda só a situação.** Nenhum outro campo é lido do corpo.
- **Qualquer transição de situação é permitida**, inclusive reabrir uma tarefa
  concluída. Máquina de estados não é requisito desta versão.
- **`updatedAt` muda em toda escrita aceita**, mesmo quando o valor enviado é
  igual ao gravado: não há detecção de alteração-nenhuma (ASM-020).
- **`createdAt` nunca muda**, em nenhuma das duas rotas.
- **Validação da edição:** título obrigatório com no máximo 120 caracteres,
  descrição com no máximo 2000, situação e prioridade obrigatórias. Prazo no
  passado é aceito na edição e recusado na criação (Q-007).

## Casos de erro

| Situação | Status | `error` |
|---|---|---|
| id inexistente em `PUT` ou `PATCH` | 404 | `TASK_NOT_FOUND` (já mapeado na 003) |
| campo obrigatório inválido | 400 | `VALIDATION_ERROR` com `fields` (já mapeado na 003) |
| corpo ilegível, ou situação fora do enum no corpo | 400 | `MALFORMED_REQUEST` (já mapeado na 003) |
| id não numérico na rota | 400 | `MALFORMED_REQUEST` (provado pela AC-020 da 003) |
| método ainda não publicado na rota | 405 | `METHOD_NOT_ALLOWED` (provado pela AC-026 da 004) |
| qualquer outra | 500 | `INTERNAL_ERROR`, sem vazar stack trace |

## Impacto técnico

Dois DTOs novos e nenhuma classe nova além deles. O resto é extensão de arquivos
que as features 003 e 004 já criaram.

| Arquivo | Origem | O que muda |
|---|---|---|
| `TaskUpdateRequest` | novo | `record` com os cinco campos e a validação da edição |
| `TaskStatusUpdateRequest` | novo | `record` de um campo, com `@NotNull` na situação |
| `TaskMapper` | T-017 da 003 | ganha `aplicar(Task, TaskUpdateRequest)`, que copia os cinco campos na entidade carregada |
| `TaskService` | T-024 da 004 | ganha `substituir(id, request)` e `alterarStatus(id, request)`, ambos carregando pela porta, aplicando e gravando |
| `TaskController` | T-025 da 004 | ganha `@PutMapping("/{id}")` e `@PatchMapping("/{id}/status")` |
| `README.md` | — | as duas rotas saem de "próxima feature" para implementada |

Decisões:

- **DTO próprio para a edição, mesmo nascendo com o formato da criação.** O
  CLAUDE.md pede um DTO por operação, e aqui a regra se paga na primeira
  feature: a divergência entre criar e editar já aparece no prazo (Q-007).
  Reaproveitar o `TaskCreateRequest` amarraria as duas operações à mesma
  validação e obrigaria a desfazer isso agora.
- **A cópia dos campos vive no `TaskMapper`, não no service.** Traduzir DTO em
  entidade é o que o mapper faz desde a 003; deixar cinco `set` seguidos no
  service misturaria orquestração com tradução. O método é `void` e recebe a
  entidade carregada — não cria instância nova, porque o id e o `createdAt` da
  tarefa gravada têm que sobreviver.
- **`buscarPorId` + `gravar`, sem `@Transactional` no service.** Cada chamada da
  porta tem a sua própria transação, e o `gravar` de uma entidade destacada com
  id vira `update`. O ciclo ler-alterar-gravar não é atômico: duas edições
  simultâneas na mesma tarefa terminam com a última sobrescrevendo a primeira
  (ASM-019).
- **A reutilização do 404 é de propósito.** `buscarPorId` do service já traduz
  ausência em `TaskNotFoundException`; as duas rotas novas passam por ele em vez
  de repetir a decisão.

## Dependências

- **Depende de:** 002-task-domain (a porta `gravar`/`buscarPorId` e os setters da
  entidade, provados pelas AC-008 a AC-011), 003-create-task (contrato de erro,
  mapper e o DTO de criação que serve de referência) e 004-list-tasks (service e
  controller onde os métodos entram).
- **Bloqueia:** 008-frontend-tasks — o formulário de edição consome o `PUT` e a
  troca rápida na listagem consome o `PATCH`.
- **Externas:** Docker rodando, para os testes de integração.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- **Exclusão (`DELETE /api/tasks/{id}`)** — é a feature 006, com as perguntas que
  são só dela (o que a segunda exclusão do mesmo id responde).
- Bloqueio otimista, `@Version` e qualquer controle de concorrência (ASM-019).
- Máquina de estados de situação: nenhuma transição é proibida nesta versão.
- `PATCH` genérico em qualquer campo (JSON Merge Patch): só a situação tem rota
  de modificação parcial, porque é a única que a listagem precisa.
- Histórico de alterações da tarefa.
- Qualquer coisa de frontend — a interface é a 008.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-018 | `PUT` é substituição total: campo opcional omitido no corpo (`description`, `dueDate`) apaga o valor gravado, em vez de preservá-lo. É o significado do verbo, e preservar o valor omitido faria o `PUT` virar um `PATCH` disfarçado — sem que o cliente tenha como apagar um campo. | confirmada | Confirmada em 27/09/2026: `PUT` é substituição total; campo opcional omitido apaga o valor gravado |
| ASM-019 | Nenhum controle de concorrência: o ciclo ler-alterar-gravar não é atômico, e duas edições simultâneas na mesma tarefa terminam com a última sobrescrevendo a primeira sem aviso. `@Version` resolveria, ao custo de uma coluna nova, uma migration e um 409 no contrato de erro — assumido agora porque não há requisito de concorrência. | confirmada | Confirmada em 27/09/2026: sem controle de concorrência; `@Version` e 409 entram só se houver requisito |
| ASM-020 | Nenhuma detecção de escrita inócua: `PATCH` com a situação que a tarefa já tem grava de novo e move o `updatedAt`. Comparar antes de gravar economizaria um `update` e custaria um `if` mais um teste; assumido porque `updatedAt` significa "última escrita aceita", não "última mudança de valor". | confirmada | Confirmada em 27/09/2026: escrita aceita move o `updatedAt`, mesmo sem mudança de valor |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-007 | A criação recusa prazo no passado (`@FutureOrPresent`, AC-015 da 003). Na edição a mesma regra tem um efeito colateral: uma tarefa que venceu não pode mais ter o título corrigido sem que o prazo seja mexido também — e a tela de edição da 008 bateria nisso em toda tarefa atrasada. Recomendação: não aplicar `@FutureOrPresent` no `TaskUpdateRequest`, porque prazo no passado é uma descrição legítima de tarefa atrasada, e a regra da criação existe para não *assumir* um compromisso vencido. O preço é que o cliente passa a poder gravar um prazo no passado de propósito. Se a resposta for manter a regra, a AC-029 se inverte antes da implementação. | respondida | Respondida em 27/09/2026: `@FutureOrPresent` não entra no `TaskUpdateRequest` — prazo no passado é aceito na edição e recusado na criação; a AC-029 fica como está |
