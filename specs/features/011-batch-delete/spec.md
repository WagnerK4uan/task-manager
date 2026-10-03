# Spec: 011 — Exclusão em lote

> feature: 011-batch-delete
> status: auditada

## Objetivo

Permitir selecionar várias tarefas no quadro e excluí-las de uma vez, numa
operação só da API, sem abrir o painel de cada uma.

## Contexto

Desde a 008, excluir é um gesto por tarefa: abrir o card, acionar "Excluir",
confirmar. Para limpar uma coluna de concluídas, isso vira dez painéis e dez
confirmações. O mantenedor pediu em 03/10/2026 a seleção múltipla com exclusão
das selecionadas.

Duas decisões foram fechadas com ele antes desta spec:

- a exclusão em lote é uma **rota da API**, atômica — ou todas saem, ou
  nenhuma —, e não um laço de `DELETE /{id}` na tela, que deixaria o quadro
  pela metade numa falha no meio;
- a seleção vive num **modo seleção** ligado por um botão, para que o quadro do
  dia a dia continue sem caixas de marcação em cada card.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-024 — Excluir várias tarefas pela API numa operação só

Como cliente da API, quero excluir um conjunto de tarefas num único pedido,
para que o resultado seja tudo ou nada e não dependa de quantos pedidos
chegaram ao servidor.

#### AC-061 — O lote exclui todas as tarefas indicadas

- **Dado** três tarefas gravadas
- **Quando** um `POST /api/tasks/batch-delete` chega com os ids de duas delas
- **Então** a resposta é 204, cada uma das duas passa a responder 404 e a
  terceira continua consultável

#### AC-062 — Lote com tarefa inexistente não exclui nada

- **Dado** duas tarefas gravadas
- **Quando** o lote chega com os ids delas e um id que não existe
- **Então** a resposta é 404 com `error` `TASK_NOT_FOUND` e as duas tarefas
  continuam consultáveis

#### AC-063 — Lote sem ids é recusado

- **Dado** a API de pé
- **Quando** o lote chega sem `ids`, com `ids` vazio ou com um `null` na lista
- **Então** a resposta é 400 com `error` `VALIDATION_ERROR` e um item em
  `fields` apontando para `ids`

#### AC-064 — Id repetido no lote conta uma vez

- **Dado** uma tarefa gravada
- **Quando** o lote chega com o id dela repetido
- **Então** a resposta é 204 e a tarefa passa a responder 404

### US-025 — Selecionar várias tarefas no quadro e excluí-las juntas

Como pessoa que usa o sistema, quero marcar várias tarefas e excluí-las com uma
confirmação só, para limpar o quadro sem abrir card por card.

#### AC-065 — O modo seleção marca cards em vez de abri-los

- **Dado** o quadro com tarefas
- **Quando** "Selecionar" é acionado
- **Então** cada card mostra uma caixa de marcação
- **E quando** dois cards são acionados
- **Então** eles ficam marcados, nenhum painel abre e a barra mostra
  "2 selecionadas"
- **E quando** "Cancelar" é acionado
- **Então** o modo termina, as caixas somem e nada fica marcado

#### AC-066 — As selecionadas saem juntas, depois de uma confirmação

- **Dado** o modo seleção com dois cards marcados e um terceiro sem marca
- **Quando** "Excluir selecionadas" é acionado
- **Então** abre a confirmação da interface citando "2 tarefas", e nenhuma
  caixa de diálogo nativa do navegador é disparada
- **E quando** a confirmação é cancelada pelo Esc
- **Então** só a confirmação fecha e os dois cards continuam marcados
- **E quando** a exclusão é acionada de novo e confirmada
- **Então** os dois cards somem do quadro, a API responde 404 para eles, o
  terceiro continua e o modo seleção termina

#### AC-067 — Marcar todas de uma coluna

- **Dado** o modo seleção, com tarefas em mais de uma coluna
- **Quando** "Marcar todas" é acionado no cabeçalho de uma coluna
- **Então** todos os cards daquela coluna ficam marcados, os das outras não, e
  o botão passa a dizer "Desmarcar todas"
- **E quando** "Desmarcar todas" é acionado
- **Então** os cards daquela coluna voltam a ficar sem marca

## Requisitos não funcionais

- **RNF-48** — Nenhuma dependência nova, no backend nem no frontend.
- **RNF-49** — Uma exclusão em lote é um único pedido HTTP e um único
  `DELETE ... WHERE id IN (...)` no banco.
- **RNF-50** — A seleção é acessível por teclado: cada card ganha um checkbox
  nativo com `aria-label` "Selecionar {título}", e a confirmação reaproveita o
  `ConfirmDialog` da 010, com foco inicial em "Cancelar" e foco preso nela.

## Regras de negócio

- **Tudo ou nada.** Se qualquer id do lote não existe, nenhuma tarefa é
  excluída (ASM-045).
- **Exclusão continua pedindo confirmação**, como a 008 fixou: a confirmação do
  lote diz quantas tarefas saem e que não há como desfazer.
- **No modo seleção o card tem um gesto só**: acioná-lo marca ou desmarca
  (ASM-047).

## Casos de erro

| Situação | Comportamento |
|---|---|
| Lote com id inexistente | 404 `TASK_NOT_FOUND`, nada é excluído |
| `ids` ausente, vazio ou com `null` | 400 `VALIDATION_ERROR` com `fields` |
| Corpo que não é JSON | 400 `MALFORMED_REQUEST` (handler existente) |
| 404 no lote, na tela | aviso, quadro recarrega, seleção perde os ids que sumiram e o modo continua (ASM-048) |
| Outra falha no lote, na tela | aviso, quadro recarrega, seleção mantida |
| Esc com a confirmação aberta | fecha só a confirmação |

## Impacto técnico

| Arquivo | O que muda |
|---|---|
| `domain/repository/TaskRepository.java` | porta ganha `contarExistentes` e `excluirPorIds` |
| `infrastructure/persistence/TaskJpaRepository.java` | consulta derivada `countByIdIn` |
| `infrastructure/persistence/TaskRepositoryAdapter.java` | implementa os dois métodos, a exclusão com `deleteAllByIdInBatch` |
| `application/dto/TaskBatchDeleteRequest.java` | novo: `record` com `ids` |
| `application/service/TaskService.java` | `excluirVarias` |
| `presentation/controller/TaskController.java` | `POST /api/tasks/batch-delete` |
| `frontend/src/app/core/services/task-api.ts` | `excluirVarias` |
| `features/tasks/pages/task-board/` | modo seleção, barra, confirmação do lote |
| `features/tasks/components/task-card/` | checkbox e gesto de marcar |
| `e2e/tests/batch-delete.spec.ts` | novo: um teste por critério de tela |
| `CLAUDE.md`, `README.md` | tabela de endpoints ganha a rota |

Decisões:

- **`POST` numa rota de ação, e não `DELETE` com corpo** (ASM-044): o corpo de
  um `DELETE` não tem semântica definida e proxies e clientes podem descartá-lo.
- **Contar antes, excluir num comando só.** O service confere quantos dos ids
  existem e só então manda o `DELETE ... IN`. A exclusão é um único comando,
  atômico por si; a janela entre contar e excluir só pode fazer uma tarefa
  sumir por outro caminho, e o resultado final é o mesmo que o lote pedia.
- **A seleção mora no quadro**, não no card: o card só recebe se está
  selecionável e se está marcado, e avisa quando é acionado.
- **A confirmação do lote é aberta pelo quadro**, que trata o Esc dela. O painel
  não abre no modo seleção, então não há disputa de Esc entre os dois.

## Dependências

- **Depende de:** 006-delete-task (semântica de exclusão), 008-kanban-board
  (quadro e harness E2E), 010-board-polish (`ConfirmDialog`).
- **Bloqueia:** nada.
- **Externas:** Docker rodando, para Testcontainers e para o compose do E2E.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- Desfazer a exclusão.
- Mover ou alterar situação em lote.
- Selecionar intervalo com Shift ou por retângulo de arrasto.
- Atalho de teclado para marcar tudo.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-044 | A rota é `POST /api/tasks/batch-delete` com corpo `{ "ids": [...] }`, respondendo 204. | confirmada | Confirmada em 03/10/2026 com a aprovação do plano pelo mantenedor |
| ASM-045 | Tudo ou nada: um id inexistente faz o lote responder 404 sem excluir nada, a mesma semântica do `DELETE /{id}`. | confirmada | Confirmada em 03/10/2026 com a aprovação do plano pelo mantenedor |
| ASM-046 | Sem limite de tamanho do lote, coerente com D-8: sem paginação, o quadro inteiro já está numa tela. | confirmada | Confirmada em 03/10/2026 com a aprovação do plano pelo mantenedor |
| ASM-047 | No modo seleção o arrastar fica desligado e as setas e o "+ Nova tarefa" somem. | confirmada | Confirmada em 03/10/2026 com a aprovação do plano pelo mantenedor |
| ASM-048 | Um 404 no lote, na tela, avisa que nada foi excluído, recarrega o quadro e tira da seleção os ids que sumiram, mantendo o modo. | confirmada | Confirmada em 03/10/2026 com a aprovação do plano pelo mantenedor |

## Perguntas em aberto

Nenhuma.
