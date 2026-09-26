# LIÇÕES — mantido pelo motor (`onp-spec licoes`)

> Não edite à mão: qualquer escrita do motor sobrescreve este arquivo.
> Estado canônico em `.spec/licoes.json`; mutação só via `onp-spec licoes`.

## Confirmadas — carregue no Especificar/Projetar

Corroboradas em múltiplas features. Aplique como guia.

_nenhuma_

## Candidatas — em observação, NÃO aplicar ainda

Vistas em uma feature só. Registradas, não confiadas.

### L-001 — Base de teste de integração compartilhada declara container único, iniciado uma vez: o ciclo de vida do @Container para o container ao fim de cada classe e o contexto Spring em cache sobrevive apontando para uma porta morta.
- sinal: `VERIFY_FALHOU` · recorrência: 1 feature(s) · escopo: `backend` · penalidades: 0
- features: 002-task-domain
- última evidência: AC-008 (002-task-domain, 2026-09-26T16:49:58.086Z)

### L-002 — Feature nova invalida a prova de todas as anteriores, porque o motor compara a data da prova com a do código mais recente: rode o verify de cada feature já auditada antes do audit --ci, não só o da feature em curso.
- sinal: `VERIFY_OBSOLETO` · recorrência: 1 feature(s) · escopo: `backend` · penalidades: 0
- features: 001-project-setup
- última evidência: — (001-project-setup, 2026-09-26T17:33:55.078Z)

## Quarentena — aplicadas e falharam, ignorar

A falha recorreu mesmo com a lição aplicada. Revisão é do usuário.

_nenhuma_
