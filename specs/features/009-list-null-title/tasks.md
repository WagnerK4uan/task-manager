# Tasks: 009 — Listagem sem filtro de título

> feature: 009-list-null-title

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula).
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  A 008 já reservou até T-049, então esta feature começa em T-050.

  A ordem é teste e depois correção, ao contrário das features anteriores: num
  defeito, teste que não falhou antes não provou que reproduz.
-->

## T-050 — Teste que reproduz o 500 da listagem sem filtro [concluida]
- Refs: US-016, AC-047
- Arquivos: backend/src/test/java/com/taskmanager/infrastructure/TaskListingWithoutFilterTest.java
- Notas: classe própria sobre a base de integração, com o contexto descartado antes dela (`@DirtiesContext`) — é o que garante fábrica de sessões nova e que o parâmetro nulo não venha tipado de carona de outra classe (RNF-33). Grava duas tarefas e pede a listagem sem filtro nenhum e só por situação, conferindo a ordem do mais recente para o mais antigo. Antes da T-051 este teste falha com `function lower(bytea) does not exist` — verificado nas duas ordens, revertendo e reaplicando a correção.

## T-051 — Dar tipo ao parâmetro de título na consulta [concluida]
- Refs: US-016, AC-047
- Arquivos: backend/src/main/java/com/taskmanager/infrastructure/persistence/TaskJpaRepository.java
- Notas: `lower(concat('%', cast(:titulo as string), '%'))`. Uma linha, nenhuma mudança de contrato e nenhum caminho novo de consulta (RNF-32). Depois dela, a T-050 fica verde e o AC-039 da 007 — a listagem pelo endereço do frontend — passa a ter prova.

<!--
  Antes do audit --ci: rodar o verify de TODAS as features. Esta correção toca a
  consulta da 004 e destrava o critério de tela da 007, então as duas precisam
  de prova nova (L-002), e cada verify sobe o compose e roda o E2E (ASM-025).
-->
