# Spec: 008 — Quadro kanban de tarefas

> feature: 008-kanban-board
> status: auditada

## Objetivo

Entregar a interface do sistema como um quadro kanban: três colunas por
situação, cards que se movem entre elas por arrasto ou por botão, e um painel
que abre o card para ler e editar todos os campos — inclusive a descrição, que
até aqui existia na API e não aparecia em lugar nenhum. Criar tarefa também
passa a ser trabalho da tela, e não mais só do `curl`.

## Contexto

O backend fechou o CRUD na 006 e a 007 entregou o harness que torna uma tela
auditável. A primeira versão desta feature entregou uma **lista** — e a lista
foi recusada pelo mantenedor em 28/09/2026: o modelo mental do produto é
quadro, não linha, e a tela precisava de cor. A implementação da lista foi
descartada antes de entrar no histórico; a spec dela existe no commit `e03a3fe`
e é substituída por esta.

O que sobreviveu da tentativa anterior, porque o quadro precisa exatamente dos
mesmos arquivos: os tipos da tarefa, o serviço com `HttpClient`, a configuração
do Tailwind e o princípio **P-006** (componente não fala HTTP), que saiu da
fila da constituição. O que morreu junto com a lista: a tela, os testes dela e
os critérios AC-040 a AC-046. Códigos de rastreio não se reaproveitam — esta
feature começa em AC-048 e T-052, e os números da lista ficam vagos para
sempre.

**O quadro muda o que a tela pede da API, não a API.** Mover um card é o
`PATCH /api/tasks/{id}/status` que a 005 publicou; salvar o painel é o
`PUT /api/tasks/{id}`; criar é o `POST /api/tasks` da 003; excluir é o
`DELETE` da 006. Nenhuma rota nova, nenhum campo novo — a 004 inclusive já
devolve tudo o que o card mostra.

**O filtro sai de cena.** A lista tinha busca por título e por situação; o
quadro mostra as três situações lado a lado, o que torna o filtro de situação
redundante, e a busca por título volta quando houver volume que a justifique
(ASM-038).

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-017 — Ver o trabalho distribuído em colunas

Como pessoa que usa o sistema, quero ver minhas tarefas em colunas por
situação, para entender de relance o que está parado, o que está andando e o
que já saiu da frente.

#### AC-048 — Cada tarefa aparece na coluna da sua situação

- **Dado** três tarefas criadas pela API, uma pendente, uma em andamento e uma
  concluída
- **Quando** o quadro é aberto
- **Então** cada uma aparece dentro da coluna da sua situação, com título,
  prioridade e prazo visíveis, e o número no topo de cada coluna é igual à
  quantidade de cards que aquela coluna mostra

#### AC-049 — Quadro sem tarefa nenhuma convida a criar, não fica mudo

- **Dado** uma API que responde a listagem com nenhuma tarefa
- **Quando** o quadro é aberto
- **Então** cada uma das três colunas mostra um convite para criar a primeira
  tarefa ali, sem nenhuma mensagem de erro, e o botão de criar continua
  disponível em cada coluna

#### AC-050 — Falha da API aparece como mensagem, não como tela branca

- **Dado** a API respondendo erro à listagem
- **Quando** o quadro é aberto
- **Então** a tela mostra uma mensagem de erro legível, sem status cru nem
  stack trace, e continua navegável

### US-018 — Mover a tarefa pelo quadro

Como pessoa que usa o sistema, quero arrastar o card para outra coluna — ou
mover por botão quando não tenho mouse —, para que mudar a situação seja o
gesto mais barato da tela.

#### AC-051 — Arrastar o card para outra coluna muda a situação no servidor

- **Dado** uma tarefa pendente no quadro
- **Quando** o card dela é arrastado para a coluna "Em andamento"
- **Então** o card passa a viver naquela coluna e **recarregar a página o
  mantém lá** — a mudança foi para o servidor, não só para a tela

#### AC-052 — Mover pelo botão do card faz o mesmo, sem mouse

- **Dado** uma tarefa pendente no quadro
- **Quando** a ação de avançar do card é acionada pelo teclado
- **Então** o card passa para a coluna seguinte e recarregar a página o mantém
  lá — o mesmo resultado do arrasto, pelo mesmo caminho no servidor

### US-019 — Abrir o card para ler e editar

Como pessoa que usa o sistema, quero abrir o card e ver a descrição inteira,
podendo aumentá-la, encurtá-la ou corrigir qualquer campo, para que a tarefa
carregue o contexto de que ela precisa.

#### AC-053 — O card abre com a descrição inteira e os campos preenchidos

- **Dado** uma tarefa com descrição longa, prioridade alta e prazo
- **Quando** o card dela é acionado no quadro
- **Então** abre um painel com a descrição inteira num campo editável, e com
  título, situação, prioridade e prazo preenchidos com o que a API devolveu

#### AC-054 — Editar e salvar reflete no quadro e sobrevive a recarregar

- **Dado** o painel aberto numa tarefa
- **Quando** a descrição é reescrita, a prioridade é trocada e o painel é salvo
- **Então** o painel fecha, o card no quadro mostra a prioridade nova, e
  reabrir o card depois de recarregar a página mostra a descrição nova

#### AC-055 — Salvar sem título é recusado com o motivo no campo

- **Dado** o painel aberto numa tarefa
- **Quando** o título é apagado e o painel é salvo
- **Então** a tela mostra a mensagem de que o título é obrigatório junto do
  campo, o painel continua aberto, e recarregar a página mostra a tarefa com o
  título antigo — nada foi gravado

#### AC-056 — Excluir pelo painel pede confirmação antes de remover

- **Dado** o painel aberto numa tarefa
- **Quando** a exclusão é acionada e a confirmação é recusada
- **Então** a tarefa continua no quadro e continua na API
- **E quando** a exclusão é acionada de novo e a confirmação é aceita
- **Então** o painel fecha, o card some do quadro e uma consulta à API responde
  404

### US-020 — Criar a tarefa já na coluna certa

Como pessoa que usa o sistema, quero criar a tarefa a partir da coluna onde ela
nasce, para não precisar criar e depois mover — nem abrir um terminal.

#### AC-057 — Criar pela coluna nasce naquela situação

- **Dado** o quadro aberto
- **Quando** a criação é acionada no topo da coluna "Em andamento" e o painel é
  preenchido com título e prioridade e salvo
- **Então** o card aparece na coluna "Em andamento", e consultar a API mostra a
  tarefa gravada com aquela situação

## Requisitos não funcionais

- **RNF-37** — O princípio P-006 continua valendo e continua sendo gate:
  `HttpClient` só em `core/services`. Card, coluna e painel não falam HTTP.
- **RNF-38** — Sem biblioteca de estado. O estado do quadro são `signal` do
  Angular; o quadro tem uma fonte só, que é a resposta da API (ASM-036).
- **RNF-39** — O arrasto usa `@angular/cdk`, do próprio time do Angular, e
  **não** uma biblioteca de terceiros. É a única dependência nova (ASM-037).
- **RNF-40** — Toda ação que muda dado vai ao servidor e o quadro recarrega da
  API. Nada de atualização otimista: o card só muda de coluna depois que o
  servidor confirmou (ASM-036).
- **RNF-41** — Os testes selecionam por papel, texto acessível ou
  `data-testid`, nunca por classe de CSS.
- **RNF-42** — Nenhuma mudança no backend. Nenhuma rota, DTO ou migration: o
  quadro consome o que a 003, a 004, a 005 e a 006 já publicaram e provaram.
- **RNF-43** — Toda ação do quadro tem caminho por teclado: mover, abrir,
  salvar, criar e excluir. Arrastar é atalho, nunca o único jeito.

## Regras de negócio

- **Coluna é situação.** Três colunas, na ordem `PENDENTE`, `EM_ANDAMENTO`,
  `CONCLUIDA` — a ordem do enum, que é a ordem do trabalho. Não existe coluna
  configurável nem situação nova: o enum é do backend.
- **A ordem dentro da coluna é a da API**, do `createdAt` mais recente para o
  mais antigo, como a AC-021 provou.
- **Mover é `PATCH /status`**; salvar o painel é `PUT`, que substitui a tarefa
  inteira; criar é `POST`; excluir é `DELETE`. A tela não inventa operação.
- **A tela não valida no lugar da API.** O título obrigatório e o limite de 120
  caracteres são regra do backend; o painel mostra o que o envelope de erro
  devolveu, por campo, em vez de duplicar a regra em TypeScript (ASM-039).
- **Situação e prioridade aparecem em português**, com os códigos do enum
  traduzidos na apresentação; o payload continua bilíngue (ASM-007 da 002).
- **Prazo é data civil** (`dd/mm/aaaa`), coerente com o `LocalDate` do D-5.
  Tarefa sem prazo mostra ausência, não uma data inventada.
- **Exclusão pede confirmação**, e quem pergunta é a interface — o backend não
  tem parâmetro de confirmação, como a 006 fixou.
- **404 é sucesso do ponto de vista de quem usa:** a tarefa já não está lá, e é
  isso que a pessoa queria.

## Casos de erro

| Situação | Comportamento na tela |
|---|---|
| API responde 4xx ou 5xx na listagem | mensagem legível, sem status cru nem stack trace (AC-050) |
| API inalcançável | a mesma mensagem: para quem usa, a diferença não é acionável |
| Falha ao mover o card | o card volta para a coluna de origem, com aviso, porque o servidor não aceitou |
| Validação recusada ao salvar | mensagem por campo, vinda do envelope de erro, com o painel aberto (AC-055) |
| 404 ao salvar ou excluir | tratado como "já não está lá": o painel fecha e o quadro recarrega, sem erro |
| Quadro sem tarefa nenhuma | convite em cada coluna, que não é erro (AC-049) |

## Impacto técnico

A feature cria a interface inteira. Tudo sob `frontend/src/app/` precisa
aparecer no `Arquivos:` de alguma tarefa: `srcGlobs` cobre esse caminho e
`ARQUIVO_ORFAO` é erro no `audit --ci`.

| Arquivo | Origem | O que muda |
|---|---|---|
| `features/tasks/models/task.ts` | da tentativa anterior | os tipos `Task`, `TaskStatus`, `TaskPriority` e o envelope de erro com `fields` |
| `core/services/task-api.ts` | da tentativa anterior | ganha `criar` e `substituir`; segue sendo o único arquivo com `HttpClient` |
| `features/tasks/components/task-card/` | novo | o card: título, prioridade, prazo, botões de mover e o acionamento do painel |
| `features/tasks/pages/task-board/` | novo | o quadro: três colunas, contadores, arrasto, estado em `signal` |
| `features/tasks/components/task-panel/` | novo | o painel lateral: ler, editar, criar e excluir |
| `app.routes.ts`, `app.config.ts`, `app.html` | T-009 da 001 | rota do quadro, `provideHttpClient` e o cabeçalho |
| `styles.css`, `frontend/.postcssrc.json` | T-009 da 001 / novo | Tailwind v4 e a paleta por situação e prioridade |
| `frontend/package.json` | T-009 da 001 | `tailwindcss`, `@tailwindcss/postcss` e `@angular/cdk` |
| `e2e/tests/kanban.spec.ts` | novo | um teste por critério, no harness da 007 |
| `README.md` | — | o quadro no bloco de estado atual |

Decisões:

- **`@angular/cdk` para o arrasto.** O `cdkDropList`/`cdkDrag` resolve
  acessibilidade, área de soltura e o marcador de posição — escrever isso à mão
  seria reimplementar uma biblioteca do próprio Angular com menos cuidado. É
  dependência de runtime, não de build, e é a única nova.
- **Arrastar e botão, os dois.** O arrasto é o gesto que define o modelo; o
  botão é o que faz a mesma coisa funcionar no teclado e no telefone (RNF-43).
  São dois caminhos para uma chamada só, e os dois têm critério próprio
  (AC-051 e AC-052).
- **Painel lateral, não página própria.** O quadro continua visível atrás, que
  é o contexto de quem está organizando trabalho. Página dedicada entraria se
  o card ganhasse histórico ou comentário — hoje não tem.
- **Um painel para editar e para criar.** Os campos são os mesmos e o backend
  aceita `POST` e `PUT` com o mesmo corpo; dois componentes quase idênticos
  seriam duplicação sem ganho. O painel sabe se tem id.
- **Cor carrega informação.** Cada coluna tem seu tom — âmbar para pendente,
  índigo para em andamento, verde para concluída — e a prioridade tem uma
  etiqueta colorida no card. Não é decoração: é o que permite achar a coluna e
  medir o peso da tarefa sem ler. O papel e o texto seguem neutros, para que a
  cor signifique alguma coisa quando aparece.
- **Sem atualização otimista.** O card muda de coluna depois da resposta do
  servidor. Custa uma ida e volta de rede antes do movimento se firmar; em
  troca, o quadro nunca mostra um estado que o banco não tem.

## Dependências

- **Depende de:** 007-e2e-proof (o harness — dependência de gate),
  001-project-setup (o esqueleto Angular e o `nginx.conf`), 003-create-task
  (`POST`), 004-list-tasks (a listagem), 005-update-task (`PUT` e
  `PATCH /status`), 006-delete-task (`DELETE`) e 009-list-null-title, sem a qual
  a primeira listagem do quadro responderia 500.
- **Bloqueia:** nada. É a última feature planejada do escopo atual.
- **Externas:** Docker rodando, para o compose que o E2E exercita.
- **Bibliotecas:** `@angular/cdk` em dependências; `tailwindcss` e
  `@tailwindcss/postcss` em devDependencies.

## Fora de escopo

- **Busca por título e filtro de situação** — o quadro mostra as três situações
  de uma vez, e a busca volta quando houver volume que a justifique (ASM-038).
- **Ordem manual dentro da coluna**: a ordem é a da API. Reordenar à mão
  exigiria um campo de posição no backend, que não existe.
- **Coluna configurável, raia, etiqueta, responsável e comentário**: o domínio
  tem oito campos e nenhum deles é dono ou marcador.
- **Destaque de tarefa atrasada** e tema escuro.
- **Testes unitários de componente**: a prova desta feature é E2E, pelo harness
  da 007.
- **Qualquer mudança de backend** (RNF-42).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-035 | O painel lateral edita todos os campos — título, descrição, situação, prioridade e prazo — e não só a descrição. Isso absorve o formulário que estava planejado para a feature seguinte, e é por isso que não existe 010: criar, ler, editar e excluir cabem no mesmo painel. O preço é uma feature maior. | confirmada | Confirmada em 28/09/2026: painel com todos os campos, decidido pelo mantenedor |
| ASM-036 | Sem biblioteca de estado e sem atualização otimista: o estado são `signal`, e toda mutação recarrega o quadro da API. Custa uma requisição a mais por ação; em troca o quadro nunca diverge do servidor, e cada critério pode ser provado com um recarregamento. | confirmada | Confirmada em 28/09/2026: `signal` e recarga pela API |
| ASM-037 | O arrasto vem do `@angular/cdk`. É dependência de runtime nova, do próprio Angular, versionada junto com o framework. A alternativa — eventos de ponteiro à mão — economizaria a dependência e custaria acessibilidade e área de soltura escritas do zero. | confirmada | Confirmada em 28/09/2026: `@angular/cdk`, com o arrasto e o botão convivendo |
| ASM-038 | O quadro não tem busca nem filtro. Com as três colunas visíveis, o filtro de situação perde sentido, e a busca por título só passa a valer quando uma coluna tiver mais cards do que cabe na tela. Enquanto isso, procurar é rolar. | confirmada | Confirmada em 28/09/2026: filtro fora do escopo desta feature |
| ASM-039 | A validação não é duplicada no TypeScript: o painel envia e mostra o que o envelope de erro devolveu, campo a campo. Custa uma ida ao servidor para descobrir que o título está vazio; em troca existe uma regra só, no lugar onde ela é obrigatória, e a tela não mente sobre o que a API aceita. | confirmada | Confirmada em 28/09/2026: validação vem do backend, exibida por campo |
| ASM-040 | O arrasto do CDK é dirigido no teste por eventos de mouse do Playwright (`mouse.down`, `mouse.move` em passos, `mouse.up`), porque o `dragTo` num salto só pode não disparar o limiar de arrasto do CDK. Se o gesto se mostrar instável no navegador headless, o critério AC-051 passa a ser provado pelo mesmo caminho de teclado do CDK, e isso fica registrado aqui — nunca removendo o critério. | confirmada | Confirmada em 28/09/2026: arrasto por eventos de mouse em passos |

## Perguntas em aberto

Nenhuma. As quatro decisões que faltavam — destino da lista, arrasto contra
botão, o que o painel edita e se criar entra nesta feature — foram respondidas
pelo mantenedor em 28/09/2026 e estão registradas acima como suposições
confirmadas.
