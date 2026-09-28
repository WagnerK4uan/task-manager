# LIÇÕES — mantido pelo motor (`onp-spec licoes`)

> Não edite à mão: qualquer escrita do motor sobrescreve este arquivo.
> Estado canônico em `.spec/licoes.json`; mutação só via `onp-spec licoes`.

## Confirmadas — carregue no Especificar/Projetar

Corroboradas em múltiplas features. Aplique como guia.

### L-002 — Feature nova invalida a prova de todas as anteriores, porque o motor compara a data da prova com a do código mais recente: rode o verify de cada feature já auditada antes do audit --ci, não só o da feature em curso.
- sinal: `VERIFY_OBSOLETO` · recorrência: 2 feature(s) · escopo: `backend` · penalidades: 0
- features: 001-project-setup, 004-list-tasks
- última evidência: — (004-list-tasks, 2026-09-26T20:18:13.115Z)

## Candidatas — em observação, NÃO aplicar ainda

Vistas em uma feature só. Registradas, não confiadas.

### L-001 — Base de teste de integração compartilhada declara container único, iniciado uma vez: o ciclo de vida do @Container para o container ao fim de cada classe e o contexto Spring em cache sobrevive apontando para uma porta morta.
- sinal: `VERIFY_FALHOU` · recorrência: 1 feature(s) · escopo: `backend` · penalidades: 0
- features: 002-task-domain
- última evidência: AC-008 (002-task-domain, 2026-09-26T16:49:58.086Z)

### L-003 — A obsolescência da prova é medida por mtime, não por conteúdo: salvar um arquivo de teste sem alterá-lo já derruba o verify — deixe o verify como último passo, depois dos commits e dos saves, imediatamente antes do audit --ci.
- sinal: `VERIFY_OBSOLETO` · recorrência: 1 feature(s) · escopo: `backend` · penalidades: 0
- features: 004-list-tasks
- última evidência: — (004-list-tasks, 2026-09-26T20:18:13.156Z)

### L-004 — Teste vermelho só prova defeito quando a mensagem é a do defeito: contexto que não sobe é erro de infraestrutura disfarçado de reprodução, então leia a falha e confirme revertendo e reaplicando a correção.
- sinal: `VERIFY_FALHOU` · recorrência: 1 feature(s) · penalidades: 0
- features: 009-list-null-title
- última evidência: AC-047 (009-list-null-title, 2026-09-28T01:16:13.508Z)

### L-005 — Verify que sai com código diferente entre execuções tem teste intermitente: ache o caso na saída completa, provoque a corrida de propósito no teste e conserte a aplicação — reexecutar até passar troca prova por sorte.
- sinal: `VERIFY_FALHOU` · recorrência: 1 feature(s) · penalidades: 0
- features: 008-frontend-tasks
- última evidência: AC-041 (008-frontend-tasks, 2026-09-28T02:42:22.223Z)

## Quarentena — aplicadas e falharam, ignorar

A falha recorreu mesmo com a lição aplicada. Revisão é do usuário.

_nenhuma_
