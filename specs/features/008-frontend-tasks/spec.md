# Spec: 008 — Listagem de tarefas na interface

> feature: 008-frontend-tasks
> status: pronta

## Objetivo

Entregar a primeira tela do sistema: a lista de tarefas, com os filtros de título
e situação, a troca rápida de situação e a exclusão. É a feature que transforma as
seis rotas do backend em algo que uma pessoa consegue usar.

## Contexto

O backend fechou o CRUD na 006, com 37 critérios provados, e a 007 entregou o
harness que torna uma tela auditável — Playwright contra o compose, com o
resultado no mesmo TAP que o motor lê. Esta feature é a primeira que usa esse
harness para provar comportamento de produto.

O que existe de frontend é o esqueleto da 001: `app.routes.ts` com um array
vazio, `app.config.ts` sem `provideHttpClient`, `styles.css` com o comentário de
exemplo do CLI. Nenhum componente, nenhum serviço, nenhum tipo. A 007 provou que
o nginx serve esse esqueleto e que o `/api/` alcança o backend (AC-038 e AC-039),
então o caminho está livre: o que falta é a tela.

**A tela não inventa regra nenhuma.** A ordem da lista é a que a AC-021 já provou
na API, os filtros são os que a AC-022 provou, o estado vazio é o 200 com array
vazio da AC-023. A interface apresenta e pede; quem decide continua sendo o
backend. Isso importa para o tamanho desta feature: ela é grande em arquivos e
pequena em decisões.

**Um laço que vem da 006.** A Q-008 fixou que a segunda exclusão do mesmo
identificador responde 404, e registrou que o preço disso é a interface tratar
esse 404 como "já não está lá" em vez de erro. A AC-046 é essa contrapartida — o
primeiro caso em que uma decisão de API tomada duas features atrás vira
comportamento observável de tela.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-014 — Ver e estreitar a lista de tarefas

Como pessoa que usa o sistema, quero ver minhas tarefas numa tela e estreitar a
lista por título ou situação, para que eu encontre o que preciso fazer sem ler a
lista inteira e sem montar requisição HTTP na mão.

#### AC-040 — A tela mostra as tarefas que a API devolve, na ordem da API

- **Dado** três tarefas criadas pela API, em momentos diferentes
- **Quando** a listagem é aberta no navegador
- **Então** as três aparecem na tela, da mais recente para a mais antiga — a
  mesma ordem que a AC-021 provou na API —, cada uma com título, situação,
  prioridade e prazo visíveis, e a situação e a prioridade em português

#### AC-041 — Os dois filtros estreitam a lista e sobrevivem a um recarregamento

- **Dado** as tarefas "Escrever a spec" (concluída) e "Revisar o PR" (pendente)
- **Quando** o filtro de título recebe `escrever`, e depois a situação recebe
  `PENDENTE`
- **Então** a tela mostra só o que casa com o filtro em cada passo, o filtro
  aparece na query string do endereço, e recarregar a página nesse endereço
  devolve a mesma lista filtrada (ASM-028)

#### AC-042 — Filtro que não casa nada mostra estado vazio, não erro

- **Dado** um filtro de título que nenhuma tarefa satisfaz
- **Quando** ele é aplicado
- **Então** a tela mostra uma mensagem de lista vazia, sem nenhuma linha de
  tarefa e sem mensagem de erro — porque 200 com array vazio é resposta de
  sucesso, como a AC-023 fixou

#### AC-043 — Falha da API aparece como mensagem, não como tela branca

- **Dado** a API respondendo erro a uma listagem
- **Quando** a tela é aberta
- **Então** ela mostra uma mensagem de erro legível, sem nenhum código de status
  cru nem stack trace, e continua navegável — nunca uma tela em branco ou um erro
  só no console

### US-015 — Agir sobre uma tarefa a partir da lista

Como pessoa que usa o sistema, quero concluir ou excluir uma tarefa direto da
lista, para que a ação mais comum não exija abrir um formulário.

#### AC-044 — Concluir a partir da lista troca a situação e a tela reflete

- **Dado** uma tarefa pendente na tela
- **Quando** a ação de concluir é acionada nela
- **Então** a linha passa a mostrar a situação concluída, e recarregar a página
  mostra a situação nova — a troca foi para o servidor pelo `PATCH`, não só para
  a tela (ASM-030)

#### AC-045 — Excluir pede confirmação antes de remover

- **Dado** uma tarefa na tela
- **Quando** a ação de excluir é acionada e a confirmação é recusada
- **Então** nada acontece: a tarefa continua na tela e continua na API
- **E quando** a ação é acionada de novo e a confirmação é aceita
- **Então** a tarefa sai da tela e uma consulta à API responde 404 — a
  confirmação é da interface, como a 006 registrou

#### AC-046 — Excluir o que já não existe não vira erro na cara de quem usa

- **Dado** uma tarefa visível na tela que foi excluída por fora, pela API
- **Quando** a exclusão é acionada nela e confirmada
- **Então** a tarefa desaparece da lista sem mensagem de erro — o 404 da rota de
  exclusão é tratado como "já não está lá", que é a contrapartida que a Q-008 da
  006 deixou para a interface

## Requisitos não funcionais

- **RNF-32** — O princípio P-006 sai da fila da constituição nesta feature, no
  mesmo commit que cria `frontend/src/app/features/**`: `HttpClient` só em
  `core/services`. Componente que fala HTTP direto quebra o gate.
- **RNF-33** — Sem biblioteca de estado. O estado da tela são `signal` do próprio
  Angular; NgRx e afins entram quando houver estado compartilhado que os
  justifique (ASM-027).
- **RNF-34** — O CSS é Tailwind v4, e a configuração é a mínima: sem
  `tailwind.config`, que a v4 dispensa. O `@angular/build` já procura config de
  PostCSS, então a integração é um `.postcssrc.json` e um `@import` (ASM-029).
- **RNF-35** — Os testes de tela selecionam por papel, texto acessível ou
  `data-testid` — nunca por classe de CSS. Com utilitários, classe deixou de
  identificar qualquer coisa.
- **RNF-36** — Nenhuma mudança no backend. Nenhuma rota nova, nenhum DTO novo,
  nenhuma migration: a tela consome o que a 003, a 004 e a 006 já publicaram e
  provaram.

## Regras de negócio

- **A tela não inventa regra.** Ordem, filtros e validação são os da API; a
  interface apresenta e pede.
- **Situação e prioridade aparecem em português** na tela, com os códigos do enum
  (`PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `BAIXA`, `MEDIA`, `ALTA`) traduzidos
  na apresentação. O payload continua bilíngue, como a ASM-007 da 002 registrou.
- **Prazo aparece como data civil** (`dd/mm/aaaa`), coerente com o `LocalDate` do
  D-5. Tarefa sem prazo mostra ausência, não uma data inventada.
- **O filtro vive na query string** da rota, não só na memória do componente: o
  endereço filtrado é recarregável e compartilhável (ASM-028).
- **Toda mutação recarrega a lista da API.** Concluir e excluir não remendam o
  array local: a tela mostra o que o servidor tem (ASM-030).
- **Exclusão pede confirmação**, e é a interface que pergunta — o backend não tem
  parâmetro de confirmação, como a 006 fixou.
- **404 na exclusão é sucesso do ponto de vista de quem usa:** a tarefa já não
  está lá, e é isso que a pessoa queria.
- **Nada de paginação**, coerente com o D-8: a tela mostra o que a API devolve.

## Casos de erro

| Situação | Comportamento na tela |
|---|---|
| API responde 4xx ou 5xx na listagem | mensagem de erro legível, sem status cru nem stack trace (AC-043) |
| API inalcançável (rede, backend fora) | a mesma mensagem: para quem usa, a diferença não é acionável |
| 404 na exclusão | tratado como "já não está lá", sem mensagem de erro (AC-046) |
| 404 na troca de situação | mensagem de erro e a lista recarregada, porque a linha visível está velha |
| Lista vazia | estado vazio, que não é erro (AC-042) |

## Impacto técnico

A primeira feature que cria código Angular. Tudo sob `frontend/src/app/` precisa
aparecer no `Arquivos:` de alguma tarefa: `srcGlobs` cobre esse caminho e
`ARQUIVO_ORFAO` é aviso no `audit` e erro no `audit --ci`.

| Arquivo | Origem | O que muda |
|---|---|---|
| `features/tasks/models/task.ts` | novo | os tipos `Task`, `TaskStatus`, `TaskPriority` e o envelope de erro da API |
| `core/services/task-api.ts` | novo | o único lugar com `HttpClient`: listar, trocar situação, excluir, e a tradução do envelope de erro |
| `features/tasks/pages/task-list/` | novo | a página: estado em `signal`, filtros ligados à query string, ações de concluir e excluir |
| `features/tasks/components/task-row/` | novo | a linha da tarefa, sem HTTP — recebe a tarefa e emite as ações |
| `app.config.ts` | T-009 da 001 | ganha `provideHttpClient` |
| `app.routes.ts` | T-009 da 001 | ganha a rota da listagem e o redirecionamento da raiz |
| `app.html`, `styles.css` | T-009 da 001 | cabeçalho da aplicação e o `@import` do Tailwind |
| `frontend/.postcssrc.json` | novo | o plugin do Tailwind v4 — a configuração inteira |
| `frontend/package.json` | T-009 da 001 | `tailwindcss` e `@tailwindcss/postcss` em devDependencies |
| `e2e/tests/task-list.spec.ts` | novo | um teste por critério, no harness da 007 |
| `specs/constituicao.md` | — | o P-006 sai da fila e entra como seção |
| `README.md` | — | a tela no bloco de estado atual |

Decisões:

- **Tailwind v4, e a justificativa não é velocidade.** Para duas telas, CSS à mão
  são umas duzentas linhas — Tailwind não resolve um problema de volume. O que ele
  resolve é o que aconteceria sem ele: a 008 e a 010 inventariam nomes de classe
  em dois lugares diferentes, com duas escalas de espaçamento e nenhum vocabulário
  comum, e no fim haveria um mini design system ad hoc que ninguém projetou.
  Tailwind troca isso por um vocabulário que já existe. O custo é duas
  devDependencies de build, um `.postcssrc.json` e um `@import`: o
  `@angular/build` já procura config de PostCSS, e as utilidades emitidas para
  duas telas ficam na casa dos kB de um dígito — os budgets de 500kB e 1MB do
  `angular.json` não são tocados. A imagem do frontend é indiferente, porque são
  dependências do estágio de build e o runtime é nginx com arquivos estáticos.
- **Os `.css` de componente desaparecem.** Com utilitários no template, um arquivo
  de estilo por componente seria um arquivo vazio — e arquivo listado numa tarefa
  que nunca nasce deixa `ARQUIVO_INEXISTENTE` pendurado no audit. Estilo que não
  couber em utilitário entra no `styles.css`, com nome, e aí é decisão
  deliberada em vez de sobra.
- **Sem interceptor.** O `core/interceptors/` que o CLAUDE.md prevê fica vazio
  nesta feature: a tradução do envelope de erro é uma função no serviço, e um
  interceptor sem autenticação, sem retry e sem correlação a fazer seria
  abstração à espera de problema (ASM-031).
- **A tradução do enum para português vive na apresentação**, num mapa simples, e
  não no serviço: é decisão de tela, e o dia que houver segundo idioma é ela que
  muda.
- **O E2E não seleciona por classe** (RNF-35). Com Tailwind isso deixou de ser
  boa prática opcional e passou a ser a única opção viável: papel, texto
  acessível ou `data-testid`.

## Dependências

- **Depende de:** 007-e2e-proof (o harness, sem o qual nenhum critério desta tela
  pode ser provado — é dependência de gate, não de código), 001-project-setup (o
  esqueleto Angular e o `nginx.conf`), 004-list-tasks (a rota de listagem com os
  filtros que a tela usa), 005-update-task (o `PATCH /status` da troca rápida) e
  006-delete-task (o `DELETE`, e a Q-008 que definiu o 404 que a AC-046 trata).
- **Bloqueia:** 010-frontend-task-form, que entra pela mesma tela no botão de
  criar e de editar, e reusa o serviço, os tipos e o vocabulário visual desta
  feature.
- **Externas:** Docker rodando, para o compose que o E2E exercita.
- **Bibliotecas:** `tailwindcss` e `@tailwindcss/postcss`, as duas em
  devDependencies do frontend. O Playwright já entrou na 007.

## Fora de escopo

- **Criar e editar tarefa** — é a 010, com o formulário reativo e a validação
  espelhando a da API.
- **Testes unitários de componente** (`@angular/build:unit-test` com Vitest): a
  prova desta feature é E2E, pelo harness da 007. O `skipTests: true` dos
  schematics no `angular.json` continua como está.
- Destaque visual de tarefa atrasada, ordenação escolhida na tela, paginação e
  busca com debounce ajustável.
- Autenticação, usuário e permissão: a tarefa não tem dono (ASM-003 da 001).
- Acessibilidade além de foco visível e rótulo em controle — o que a RNF-35 já
  exige por via dos seletores de teste — e tema escuro.
- Responsividade além de a tela não quebrar em largura de telefone.
- Qualquer mudança de backend (RNF-36).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-027 | Sem biblioteca de estado: o estado da tela são `signal` do Angular, e a lista tem uma fonte só — a resposta da API. NgRx resolveria um problema de estado compartilhado entre telas distantes, que não existe com duas telas. | confirmada | Confirmada em 27/09/2026: `signal` bastam; NgRx entra se houver estado compartilhado |
| ASM-028 | O filtro é espelhado na query string da rota, e a tela lê o filtro de lá. Custa ler e escrever parâmetro de rota; em troca o endereço filtrado é recarregável e compartilhável, e é o que a AC-041 exige. Sem isso, recarregar a página perderia o filtro. | confirmada | Confirmada em 27/09/2026: filtro na query string, recarregável |
| ASM-029 | O CSS é Tailwind v4, com `tailwindcss` e `@tailwindcss/postcss` em devDependencies do frontend, um `.postcssrc.json` e um `@import` no `styles.css` — a v4 dispensa `tailwind.config`. A justificativa está em "Impacto técnico": não é velocidade, é não deixar a 008 e a 010 inventarem um design system ad hoc em dois lugares. O preço são duas dependências de build e utilitários no template em vez de nomes semânticos de classe. | confirmada | Confirmada em 27/09/2026: Tailwind v4 aprovado pelo mantenedor, com a justificativa registrada |
| ASM-030 | Toda mutação recarrega a lista da API, em vez de atualizar o array local. Custa uma requisição a mais por ação; em troca a tela nunca diverge do servidor, e a AC-044 pode ser provada com um recarregamento. | confirmada | Confirmada em 27/09/2026: recarrega da API; sem atualização otimista |
| ASM-031 | Nenhum interceptor: a tradução do envelope de erro é função do serviço. `core/interceptors/` nasce vazio e ganha conteúdo na feature que tiver autenticação, retry ou correlação para resolver. | confirmada | Confirmada em 27/09/2026: sem interceptor enquanto não houver problema concreto |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-011 | A AC-043 pede que falha da API apareça como mensagem, e o E2E precisa de um jeito de causar essa falha contra o compose. Recomendação: interceptar a rota no navegador (`page.route` do Playwright, devolvendo 500), em vez de derrubar o backend ou apontar a tela para um endereço inválido. É determinístico, não mexe no estado do compose e não deixa o container num estado que o teste seguinte herda. O preço é que o erro é simulado na borda do navegador, não no servidor — mas o que esta AC observa é o comportamento da tela, e para isso a origem do 500 é indiferente. | respondida | Respondida em 27/09/2026: `page.route` devolvendo 500; sem derrubar o backend |
