# Spec: 010 — Acabamento do quadro

> feature: 010-board-polish
> status: auditada

## Objetivo

Fechar três arestas do quadro da 008 que quebram a identidade visual: a
confirmação de exclusão que abre a caixa nativa do navegador, o favicon padrão
do Angular na aba e o painel lateral que aparece de uma vez, sem transição.

## Contexto

A 008 entregou o quadro e provou cada gesto dele. O que ficou para trás não é
comportamento, é acabamento. O mantenedor apontou os três pontos em 28/09/2026:

- a exclusão usa `window.confirm`, que mostra "localhost:4200 diz" numa caixa
  que o sistema não desenha nem estiliza — a única tela do produto fora da
  paleta;
- a aba carrega o `favicon.ico` que o `ng new` gerou, o que faz o sistema se
  apresentar como "um projeto Angular" e não como ele mesmo;
- o painel lateral surge no lugar sem movimento, e o olho perde de onde ele
  veio.

Nenhum dos três muda o que a tela pede da API. A regra de negócio "exclusão
pede confirmação" (008) continua a mesma; muda quem desenha a pergunta.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-021 — Confirmar a exclusão sem sair do visual do quadro

Como pessoa que usa o sistema, quero que a pergunta "excluir?" seja parte da
tela, e não uma caixa do navegador, para que a ação mais destrutiva do produto
tenha o mesmo cuidado visual do resto.

#### AC-058 — A exclusão pergunta pela própria interface

- **Dado** o painel aberto numa tarefa
- **Quando** a exclusão é acionada
- **Então** abre uma confirmação desenhada pelo sistema, que cita o título da
  tarefa, e nenhuma caixa de diálogo nativa do navegador é disparada
- **E quando** a confirmação é cancelada pelo Esc
- **Então** só a confirmação fecha: o painel continua aberto e a tarefa
  continua na API
- **E quando** a exclusão é acionada de novo e confirmada
- **Então** o painel fecha, o card some do quadro e uma consulta à API responde
  404

### US-022 — Reconhecer a aba do sistema

Como pessoa que usa o sistema, quero que a aba mostre o ícone do próprio
produto, para achá-la entre as outras e não confundi-la com qualquer projeto
Angular.

#### AC-059 — A aba usa o ícone do projeto

- **Dado** o sistema servido pelo compose
- **Quando** o quadro é aberto
- **Então** o ícone declarado pelo documento é o `favicon.svg` do projeto, e
  esse endereço responde 200 com o tipo `image/svg+xml`

### US-023 — Ver o painel chegar

Como pessoa que usa o sistema, quero que o painel entre deslizando da borda,
para entender de onde ele veio e que o quadro continua ali atrás.

#### AC-060 — O painel entra com animação, e respeita quem pediu menos movimento

- **Dado** uma tarefa no quadro
- **Quando** o card dela é acionado
- **Então** o painel abre com uma animação de entrada ativa
- **E dado** o navegador com a preferência de movimento reduzido
- **Quando** o card é acionado
- **Então** o painel abre sem animação

## Requisitos não funcionais

- **RNF-44** — Nenhuma dependência nova. O foco preso na confirmação vem do
  `cdkTrapFocus` de `@angular/cdk/a11y`, que já está no projeto desde a 008
  (ASM-037).
- **RNF-45** — A animação é só CSS: dura em torno de 220 ms e desliga com
  `prefers-reduced-motion: reduce`, como o destaque de card mexido já faz.
- **RNF-46** — A confirmação é acessível por teclado: abre com o foco em
  "Cancelar", o foco não escapa dela enquanto está aberta, e o Esc cancela.
- **RNF-47** — Nenhuma mudança no backend.

## Regras de negócio

- **Exclusão continua pedindo confirmação**, como a 008 fixou. A pergunta
  agora é da interface, com o título da tarefa e a advertência de que não há
  como desfazer.
- **Cancelar é o caminho seguro**: é ele que recebe o foco quando a confirmação
  abre, para que um Enter distraído não apague nada.

## Casos de erro

| Situação | Comportamento na tela |
|---|---|
| Esc com a confirmação aberta | fecha só a confirmação; o painel continua (ASM-041) |
| Clique fora da confirmação | equivale a cancelar |
| Falha ou 404 ao excluir | o mesmo da 008: aviso ou silêncio, painel fecha e quadro recarrega |

## Impacto técnico

| Arquivo | O que muda |
|---|---|
| `frontend/public/favicon.svg` | novo: o quadro em miniatura, três colunas nas cores das situações |
| `frontend/public/favicon.ico` | regenerado a partir do SVG, em 16, 32 e 48 px, no lugar do ícone padrão do Angular |
| `frontend/src/index.html` | o `link rel="icon"` aponta para o SVG |
| `frontend/src/app/shared/confirm-dialog/` | novo: a confirmação reutilizável |
| `features/tasks/components/task-panel/` | o painel abre a confirmação e trata o Esc em dois níveis |
| `features/tasks/pages/task-board/task-board.ts` | sai o `window.confirm` |
| `frontend/src/styles.css` | animação de entrada do painel e do véu |
| `e2e/tests/kanban-polish.spec.ts` | novo: um teste por critério |
| `e2e/tests/kanban.spec.ts` | o teste do AC-056 passa a responder a confirmação da interface |

Decisões:

- **A confirmação é um componente em `shared/`**, e não do painel: ela não
  sabe o que é tarefa, recebe título, mensagem e rótulo da ação. Hoje só a
  exclusão a usa; o lugar certo dela é o de coisa que não pertence a uma
  feature.
- **Quem abre a confirmação é o painel**, porque é ele que já trata o Esc. Se a
  confirmação morasse no quadro, o Esc chegaria aos dois e fecharia o painel
  junto (ASM-041).
- **Favicon em SVG, com o `.ico` gerado dele.** O documento declara o SVG, que
  escala e é texto versionável. O `.ico` continua existindo porque navegadores
  e leitores de feed pedem `/favicon.ico` por conta própria, e sem ele o
  `try_files` do nginx responderia o `index.html` nesse endereço. Ele é
  derivado do SVG (`magick -density 384 favicon.svg -define
  icon:auto-resize=48,32,16 favicon.ico`), nunca desenhado à parte.
- **Animação só de entrada** (ASM-042), em CSS puro, no mesmo padrão do
  `card-mexido` da 008.

## Dependências

- **Depende de:** 008-kanban-board (o quadro, o painel e o harness E2E).
- **Bloqueia:** nada.
- **Externas:** Docker rodando, para o compose que o E2E exercita.
- **Bibliotecas:** nenhuma nova.

## Fora de escopo

- Animação de saída do painel.
- Desfazer exclusão.
- Ícones para instalação como aplicativo (manifest, apple-touch-icon).
- Tema escuro.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-041 | A confirmação é aberta pelo painel, que já escuta o Esc. Com ela aberta, o Esc cancela só a confirmação; sem ela, fecha o painel como antes. | confirmada | Confirmada em 28/09/2026 com a aprovação do plano pelo mantenedor |
| ASM-042 | A animação é só de entrada. A saída não foi pedida, e fazê-la exigiria segurar o painel no DOM depois do `@if` desligar. | confirmada | Confirmada em 28/09/2026 com a aprovação do plano pelo mantenedor |
| ASM-043 | O AC-056 da 008 continua valendo sem mudar de texto: ele fala em "confirmação", não em caixa nativa. O teste dele troca o tratamento do `dialog` do navegador por cliques na confirmação da interface, com as mesmas asserções. | confirmada | Confirmada em 28/09/2026 com a aprovação do plano pelo mantenedor |

## Perguntas em aberto

Nenhuma.
