# Tasks: 007 — Prova executável da interface

> feature: 007-e2e-proof

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 006 terminou
  em T-038, então esta feature começa em T-039.

  A ordem é de fora para dentro, ao contrário das features de backend: primeiro
  o ferramental que sabe rodar e reportar, depois os testes que ele carrega. A
  T-041 é a única que produz prova, e é a última.
-->

## T-039 — Pacote de E2E com Playwright [concluida]
- Refs: US-013
- Arquivos: e2e/package.json, e2e/package-lock.json, e2e/tsconfig.json, e2e/playwright.config.ts, e2e/.gitignore
- Notas: pacote próprio na raiz, com `@playwright/test` e mais nada — fora do `frontend/package.json` pela razão da RNF-28. A config aponta o `baseURL` para o frontend do compose, usa o reporter `junit` com `outputFile` num arquivo de prefixo `TEST-` (é o que o `lerRelatorios` do `spec-tap.mjs` filtra) e roda só Chromium. O `.gitignore` cobre `node_modules/` e o diretório de resultados.

## T-040 — O spec-tap sobe o compose, roda o E2E e mescla o TAP [concluida]
- Refs: US-013
- Arquivos: scripts/spec-tap.mjs, onpspec.config.json
- Notas: o script passa a subir o compose com build, rodar o E2E depois do build do backend, concatenar os casos dos dois relatórios antes de imprimir o TAP e derrubar o compose ao final — inclusive quando o teste falha. O `lerRelatorios` já serve para os dois, porque lê o atributo `name` do `<testcase>`. O exit code considera as duas suítes. Um interruptor de ambiente pula o E2E; pular devolve `AC_SEM_PROVA`, nunca um falso PASS (ASM-025). No `onpspec.config.json`, `testGlobs` ganha `e2e/**` — sem isso o motor não encontra as etiquetas `@spec:` e todo critério de tela cai em `AC_SEM_TESTE`. As duas mudanças vão juntas porque uma sem a outra não produz prova nenhuma.

## T-041 — Prova de que o compose está ligado [concluida]
- Refs: US-013, AC-038, AC-039
- Arquivos: e2e/tests/stack.spec.ts
- Notas: dois testes, cada um com o título começando em `@spec:AC-xxx`. A AC-038 abre a raiz do frontend e verifica 200 de HTML com o `app-root` no documento — não afirma nada sobre rota, porque o `app.routes.ts` ainda está vazio e é a 008 que o preenche. A AC-039 cria uma tarefa pela API e pede `GET /api/tasks` **pelo endereço do frontend**, provando o `proxy_pass`; o dado é criado pelo próprio teste, com título único (ASM-026). Nenhuma asserção sobre ausência de erro no console: com rotas vazias o router reclama, e isso é esperado nesta feature. O verify é o último passo, depois dos commits e dos saves (L-003).

## T-042 — Documentação da exceção ao D-3 [concluida]
- Refs: US-013
- Arquivos: CLAUDE.md, README.md
- Notas: no CLAUDE.md, a seção "Testes" e o D-3 deixam de dizer que o frontend não tem prova e passam a registrar a exceção e o motivo; a estrutura de diretórios ganha o `e2e/`; a tabela de stack ganha Playwright. No README, como rodar o E2E e o aviso de que o `verify` agora sobe o compose. Tarefa de documentação, separada porque não altera código da feature.

<!--
  Antes do audit --ci: rodar o verify de TODAS as features, não só desta —
  um commit desta feature invalida a prova das anteriores por mtime (L-002), e
  a partir daqui cada verify também sobe o compose e roda o E2E (ASM-025).
-->
