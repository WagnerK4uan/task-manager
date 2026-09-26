# Constituição — v2.1.0

<!--
  Princípios inegociáveis do projeto. Não são estilo: são restrições.
  P-xxx = princípio (código de rastreio, como US/AC/T).
  Níveis: [DEVE] obrigatório · [RECOMENDADO] forte · [PODE] permitido/explícito.
  Todo [DEVE] precisa de verificação executável — senão o audit acusa
  "princípio sem verificação" (PRINCIPIO_SEM_VERIFICACAO). Formatos:
    - verificação(gate): satisfeita pelo próprio audit (só p/ princípios "meta")
    - verificação(teste): @principle:P-xxx
    - verificação(proibido): `regex` em `glob`
    - verificação(obrigatório): `regex` em `glob`

  REGRA DE ENTRADA: um princípio só sai da fila quando a verificação dele
  consegue rodar. Glob que não casa nenhum arquivo é verificação inerte — o
  audit acusa (GLOB_SEM_ARQUIVOS) e o princípio vira decoração. Por isso cada
  princípio é ativado na mesma tarefa que cria o código que ele guarda; a fila
  está no fim do arquivo, com a feature de entrada de cada um.
-->

## P-001 [DEVE] Todo requisito tem prova executável

Nenhuma feature é declarada pronta sem o audit em modo CI sair limpo (exit 0).
Cada critério de aceite tem um teste que carrega `@spec:AC-xxx` no título, e
quem decide se o critério passou é o test runner — nunca uma afirmação em
texto.

- verificação(gate): intrínseca ao audit

---

## P-002 [DEVE] Schema de banco é versionado, nunca gerado

A migration é a única fonte do schema. O Flyway cria e evolui as tabelas; o
Hibernate fica em `validate` e aborta a inicialização quando o mapeamento
divergir do que está no banco. `create`, `create-drop` e `update` ficam de
fora de qualquer perfil — inclusive o de teste, porque um schema gerado pelo
ORM prova um banco que não é o de produção.

- verificação(proibido): `ddl-auto:\s*(create|create-drop|update)` em `backend/src/main/resources/**`

---

## P-003 [RECOMENDADO] Segredos nunca em código

Credencial chega por variável de ambiente, nunca literal em arquivo
versionado. O perfil declara a referência (`${DB_PASSWORD}`); quem fornece o
valor é o ambiente. O repositório versiona apenas o `.env.example`, com nomes
e sem valores.

- verificação(proibido): `(api[_-]?key|senha|password|secret)\s*[:=]\s*['"][^'"$\{][^'"]{7,}` em `backend/src/main/resources/**`

---

## Fila de princípios

Ainda não estão ativos porque o código que eles guardam não existe. Cada um
entra como seção `## P-xxx [NÍVEL]` na tarefa indicada, no mesmo commit que
cria os arquivos do glob.

| Princípio | Verificação | Entra em |
|---|---|---|
| P-004 [DEVE] Controller não fala com repositório — toda operação passa pelo service | proibido `Repository` em `backend/src/main/java/**/presentation/**` | 003-create-task |
| P-005 [DEVE] Entidade JPA não é payload da API — a apresentação só conhece DTOs | proibido `domain\.entity` em `backend/src/main/java/**/presentation/**` | 003-create-task |
| P-006 [DEVE] Componente Angular não faz chamada HTTP — HttpClient vive em core/services | proibido `HttpClient` em `frontend/src/app/features/**` | 008-frontend-tasks |
