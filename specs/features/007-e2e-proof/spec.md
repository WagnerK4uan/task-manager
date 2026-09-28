# Spec: 007 — Prova executável da interface

> feature: 007-e2e-proof
> status: auditada

## Objetivo

Dar ao projeto o mecanismo que torna uma feature de frontend auditável: um pacote
de testes de ponta a ponta com Playwright, cujo resultado entra no mesmo TAP que
o motor já lê. E, de passagem, provar duas coisas que o `docker-compose.yml`
promete desde a 001 e que nenhum teste verifica — que o nginx serve a aplicação
Angular e que o `proxy_pass` de `/api/` alcança o backend.

## Contexto

O backend fechou o CRUD na 006, com 37 critérios provados. O frontend é o
esqueleto que o CLI gerou: `app.routes.ts` com um array vazio. A 001 declarou
"qualquer tela ou componente Angular além do esqueleto" fora de escopo e não
criou critério nenhum sobre o frontend — é por isso que o gate ficou verde até
aqui sem nunca ter provado uma linha de interface.

**A tela da 008 não tem essa saída.** O motor exige um teste `@spec:AC-xxx` para
todo critério de aceite, sem exceção de camada — o laço é `for (const ac of
allAcs(spec))` em `audit.js`, e `AC_SEM_TESTE` é erro. O `testGlobs` do
`onpspec.config.json` hoje só olha `backend/src/test/**`. Uma tela com critérios
e sem teste deixaria o `audit --ci` vermelho para sempre; uma tela sem critérios
seria a especificação-como-ficção que o D-1 existe para impedir.

A decisão do mantenedor, em 27/09/2026, foi provar a interface com Playwright
contra o compose de pé. É a mesma escolha que o D-9 fez no backend ao recusar o
H2: testar contra o que não é produção prova um sistema que não existe. O D-3 do
CLAUDE.md ("testes só no backend") passa a ter exceção registrada, e o motivo é
que o custo mudou de lado — até a 006 não havia tela; a partir daqui, não provar
a tela é entregar metade do sistema sem gate.

**Esta feature é o harness, separado da tela de propósito.** Ela não toca uma
linha de Angular: mexe no `onpspec.config.json`, no `scripts/spec-tap.mjs` e cria
um pacote novo na raiz. Vive sozinha porque se prova sozinha — o caminho
navegador → nginx → build → `/api/` → backend existe desde a 001 e nunca foi
exercitado. O `TaskApiIntegrationTest` fala com a aplicação Spring direto, sem
nginx no meio, então uma quebra no `nginx.conf` ou no estágio de build do
`frontend/Dockerfile` passaria por todos os 37 critérios sem ser notada.

## Requisitos funcionais

Expressos como histórias e critérios de aceite abaixo.

## Histórias

### US-013 — Saber que os dois artefatos estão de fato ligados

Como pessoa que mantém o projeto, quero que o caminho do navegador até o banco
seja exercitado por teste, para que uma quebra no nginx ou no build da imagem do
frontend apareça no gate em vez de aparecer no primeiro acesso de alguém.

#### AC-038 — O nginx serve a aplicação Angular compilada

- **Dado** o compose de pé, com as imagens construídas
- **Quando** a raiz do frontend é aberta no navegador
- **Então** a resposta é 200 de HTML, o `app-root` está no documento e o pacote
  JavaScript executou — o que prova que o estágio de build do Dockerfile gerou o
  bundle e que o nginx o está servindo do lugar certo

#### AC-039 — O `/api/` do frontend alcança o backend

- **Dado** o compose de pé e uma tarefa criada pela API
- **Quando** `GET /api/tasks` é pedido **através do endereço do frontend**, não
  do backend
- **Então** a resposta é 200 com um array JSON que contém essa tarefa — o
  `proxy_pass` do `nginx.conf` está ligado ao serviço certo, e a interface da 008
  pode contar com endereços relativos

## Requisitos não funcionais

- **RNF-28** — O harness **não** entra no `frontend/package.json`. O `npm ci` do
  `frontend/Dockerfile` instala devDependencies, e o postinstall do Playwright
  baixa navegadores — que em Alpine nem executam. O pacote vive na raiz, em
  `e2e/` (ASM-024).
- **RNF-29** — A prova do E2E entra no mesmo TAP do Surefire, pelo
  `scripts/spec-tap.mjs`: critério de tela é decidido pelo test runner, nunca por
  afirmação em texto. O D-4 continua valendo, com duas fontes em vez de uma.
- **RNF-30** — Nenhuma mudança no backend e nenhuma linha de Angular. Não há rota
  nova, DTO novo, migration nem componente: esta feature é infraestrutura de
  prova.
- **RNF-31** — Teste pulado não é prova, e continua não sendo: o `spec-tap.mjs` já
  imprime `# SKIP`, que o motor trata como ausência de veredito. O E2E não muda
  essa regra.

## Regras de negócio

Esta feature não tem regra de negócio — não há comportamento de produto nela. O
que ela tem são regras de prova:

- **O título do teste carrega a etiqueta.** Um teste E2E se chama
  `@spec:AC-xxx ...`, exatamente como um `@DisplayName` do backend.
- **O E2E roda contra o compose**, com as imagens construídas — não contra
  `ng serve`, que serviria código que não passou pelo Dockerfile.
- **O teste cria os próprios dados pela API**, com títulos únicos, e não assume
  banco vazio nem conta o total de linhas: o volume `postgres-data` é persistente
  (ASM-026).
- **Pular o E2E nunca produz PASS.** Sem os casos no TAP, os critérios que
  dependem dele voltam como `AC_SEM_PROVA` e o gate fica vermelho.

## Casos de erro

| Situação | Comportamento |
|---|---|
| Compose não sobe, ou o backend não fica saudável | o `spec-tap.mjs` não produz caso de E2E e sai diferente de zero; a cauda da saída é impressa como comentário TAP |
| Navegador do Playwright ausente na máquina | o E2E falha na largada, com a mensagem do próprio Playwright na cauda do TAP |
| Teste E2E falha | `not ok` no TAP, `VERIFY_FALHOU` no motor, e o critério correspondente não recebe prova |
| E2E desligado pelo interruptor de ambiente | nenhum caso de E2E no TAP; os critérios que dependem dele viram `AC_SEM_PROVA` (ASM-025) |

## Impacto técnico

Nenhuma classe, nenhum componente. Um pacote novo e dois arquivos de
configuração do próprio ferramental de specs.

| Arquivo | Origem | O que muda |
|---|---|---|
| `e2e/package.json` | novo | pacote próprio, com `@playwright/test` e `@types/node` |
| `e2e/tsconfig.json` | novo | diz ao editor como tratar a pasta: alvo, `types: ["node"]` e `skipLibCheck` |
| `e2e/playwright.config.ts` | novo | `baseURL` do frontend do compose, reporter `junit` num arquivo `TEST-*.xml`, só Chromium |
| `e2e/.gitignore` | novo | ignora `node_modules/` e o diretório de resultados |
| `e2e/tests/stack.spec.ts` | novo | os dois testes de fumaça das AC-038 e AC-039 |
| `scripts/spec-tap.mjs` | T-010 da 001 | sobe o compose, roda o E2E, mescla os casos no mesmo TAP e considera as duas suítes no exit code |
| `onpspec.config.json` | T-001 da 001 | `testGlobs` ganha `e2e/**` |
| `CLAUDE.md` | — | o D-3 e a seção "Testes" ganham a exceção registrada; a estrutura de diretórios ganha o `e2e/` |
| `README.md` | — | como rodar o E2E |

Decisões:

- **Pacote próprio na raiz, não no `frontend/`.** Pela RNF-28, e também porque o
  E2E não é código de nenhum dos dois artefatos: é o teste que exercita os dois
  juntos. A raiz é onde ele pertence. De quebra, o `srcGlobs` não o alcança —
  logo ele não vira `ARQUIVO_ORFAO`, que é erro sob `--ci`.
- **O `spec-tap.mjs` não precisa de parser novo.** O `lerRelatorios` já lê o
  atributo `name` do `<testcase>` e imprime `ok N - <título>`; o reporter `junit`
  do Playwright põe o título do teste nesse mesmo atributo. Basta o arquivo ter o
  prefixo `TEST-`, que é o que a função filtra. Nada muda no lado do onp-spec
  além do `testGlobs`.
- **O harness não tem critério de aceite sobre si mesmo.** Um detector de falha
  não se prova com o que ele detecta sem sabotagem deliberada. As AC-038 e AC-039
  são sobre o compose, não sobre o harness; que a mesclagem funciona fica provado
  pelo uso — se o TAP não trouxesse o E2E, esses dois critérios voltariam como
  `AC_SEM_PROVA`.
- **`@types/node` e um `tsconfig.json` no pacote.** Os tipos do próprio Playwright
  referenciam `Buffer`, `fs`, `stream` e `child_process` do Node; sem as
  definições e sem um `tsconfig.json`, o editor acusa erro em cada arquivo do
  `e2e/` — ainda que o teste rode, porque o Playwright transpila sem checar tipo.
  Prova que o editor marca de vermelho é prova que ninguém mantém. `typescript`
  **não** entra: o editor traz o seu, e o Playwright não precisa de compilador.
- **Só Chromium.** Provar a mesma tela em três motores de navegador não é o risco
  deste projeto, e triplicaria o tempo de todo `verify`. Um segundo navegador
  entra quando houver relato de defeito que só aparece nele.

## Dependências

- **Depende de:** 001-project-setup (o `docker-compose.yml`, o `nginx.conf`, o
  `frontend/Dockerfile` e o `scripts/spec-tap.mjs`) e 006-delete-task, por ser o
  estado em que o backend está completo — a AC-039 usa a rota de listagem.
- **Bloqueia:** 008-frontend-tasks e 010-frontend-task-form. Sem este harness,
  nenhum critério de tela pode ser provado e as duas features seriam
  inauditáveis.
- **Externas:** Docker rodando, para o compose. Os navegadores do Playwright são
  baixados uma vez, fora da imagem do frontend.
- **Bibliotecas:** `@playwright/test` e `@types/node`, no pacote `e2e/` e em nenhum
  outro lugar.

## Fora de escopo

- **Qualquer tela** — a listagem é a 008 e o formulário é a 010. Esta feature
  entrega prova, não interface.
- **Testes unitários de componente** (`@angular/build:unit-test` com Vitest): o
  `skipTests: true` dos schematics no `angular.json` continua como está.
- Segundo navegador, teste de responsividade e captura de tela como artefato de
  falha configurada além do padrão.
- Paralelismo entre arquivos de teste: com dois testes não há o que paralelizar,
  e um banco compartilhado torna paralelismo uma fonte de intermitência.
- Ambiente de E2E separado do compose de desenvolvimento, com banco próprio e
  semeadura determinística (ASM-026).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-024 | O harness é um pacote na raiz (`e2e/`), fora do `frontend/package.json`, porque o `npm ci` do `frontend/Dockerfile` instala devDependencies e o Playwright baixa navegadores no postinstall — em Alpine, onde eles nem rodam. O preço é um terceiro `package.json` no repositório. | confirmada | Confirmada em 27/09/2026: pacote próprio em `e2e/`, fora do frontend |
| ASM-025 | Todo `verify` passa a pagar o custo do E2E — compose de pé e navegador —, porque o `testCommand` do motor é global: o `spawnSync(config.testCommand)` do `verify.js` não recebe a feature. A L-002 obriga a reverificar as features anteriores a cada feature nova, então o custo é pago uma vez por feature. Um interruptor de ambiente permite pular o E2E, e pular devolve `AC_SEM_PROVA` para os critérios de tela — nunca um falso PASS. | confirmada | Confirmada em 27/09/2026: custo aceito, com interruptor que só sabe subtrair prova |
| ASM-026 | O E2E roda contra o compose de desenvolvimento, cujo volume `postgres-data` é persistente: os testes criam os próprios dados pela API, com títulos únicos, e não assumem banco vazio nem contam o total de linhas. É a mesma disciplina que o `TaskApiIntegrationTest` já segue. Um ambiente de E2E com banco próprio e semeadura determinística seria mais robusto, ao custo de um segundo compose para manter. | confirmada | Confirmada em 27/09/2026: teste cria os próprios dados; sem compose separado |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-010 | Quem sobe o compose: o `spec-tap.mjs` ou a pessoa que roda o `verify`? Exigir o compose já de pé deixa o `verify` bem mais rápido no uso do dia a dia, e é o que a maioria dos projetos faz. Recomendação: **o `spec-tap.mjs` sobe e derruba**, porque prova que depende de estado de ambiente não é prova — se o compose estiver de pé com uma imagem velha, o E2E aprova código que não é o do commit, e o motor não tem como saber. É o mesmo princípio do Testcontainers no backend, que sobe o Postgres em vez de exigir um rodando. O preço é o tempo de `docker compose up --build` em cada `verify`. | respondida | Respondida em 27/09/2026: o `spec-tap.mjs` sobe e derruba o compose — prova não depende de estado de ambiente |
