# Constituição — v2.3.0


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

## P-004 [DEVE] Controller não fala com repositório

Toda operação passa pelo service. O controller recebe a requisição, entrega o
DTO e devolve a resposta; buscar, gravar e excluir são decisões da camada de
aplicação. Um controller que injeta repositório empurra regra de negócio para a
borda e deixa o service sem motivo para existir.

- verificação(proibido): `Repository` em `backend/src/main/java/**/presentation/**`

---

## P-005 [DEVE] Entidade JPA não é payload da API

A apresentação só conhece DTOs. Expor a entidade acopla o contrato público ao
mapeamento do banco — renomear uma coluna viraria quebra para quem consome. O
caminho inverso é pior: aceitar a entidade na entrada deixaria o chamador
escolher `id`, `createdAt` e `updatedAt`, que são do servidor.

- verificação(proibido): `domain\.entity` em `backend/src/main/java/**/presentation/**`

---

## P-006 [DEVE] Componente Angular não faz chamada HTTP

`HttpClient` vive em `core/services`. Componente que chama a API direto mistura
apresentação com transporte: a tela passa a conhecer rota, envelope de erro e
formato de resposta, e a mesma tradução de erro aparece copiada em cada lugar que
pede dado. O serviço é o único ponto que sabe falar com a API, e é por isso que a
tela pode ser trocada sem tocar no transporte.

- verificação(proibido): `HttpClient` em `frontend/src/app/features/**`

---

## Fila de princípios

Nenhum princípio na fila: todos os que o projeto declarou estão ativos. Princípio
novo entra aqui quando o código que ele guarda ainda não existe, e sai para
seção no mesmo commit que cria os arquivos do glob.
