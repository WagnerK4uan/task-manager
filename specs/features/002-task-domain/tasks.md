# Tasks: 002 — Domínio da tarefa e porta de persistência

> feature: 002-task-domain

<!--
  T-xxx = tarefa · Refs: histórias/critérios que a tarefa atende
  Arquivos: o que a tarefa cria ou altera (separados por vírgula) — é o que
  decide o que pode rodar em paralelo e o que o audit considera código órfão.
  Uma tarefa = um commit. Status: pendente | em-andamento | concluida

  Códigos de rastreio nunca são renumerados nem reaproveitados: a 001 usou
  T-001 a T-010, então esta feature começa em T-011.

  A ordem é de dentro para fora — enums, entidade, porta, adaptador —, porque
  cada passo compila sozinho e o seguinte depende do anterior. A prova vem por
  último, como na 001: a partir de T-016 os critérios saem do papel.
-->

## T-011 — Enums de status e prioridade [pendente]

- Refs: US-004, AC-009
- Arquivos: backend/src/main/java/com/taskmanager/domain/enums/TaskStatus.java, backend/src/main/java/com/taskmanager/domain/enums/TaskPriority.java
- Notas: dois enums sem comportamento, com os valores em português fixados na ASM-007. Nada de campo de rótulo nem de `fromString`: o texto que vai ao banco é o nome da constante, e quem converte o JSON é o Jackson. São as primeiras classes do domínio — a partir daqui as regras ArchUnit da 001 param de rodar sobre pacote vazio.

## T-012 — Entidade Task mapeada na tabela existente [pendente]

- Refs: AC-008, AC-010
- Arquivos: backend/src/main/java/com/taskmanager/domain/entity/Task.java
- Notas: mapeia a `tasks` criada pela `V1`, coluna a coluna, sem tocar em migration (RNF-08). Construtor de negócio com os cinco campos que o chamador informa, construtor protegido sem argumentos para o JPA, acessores explícitos (sem Lombok) e nenhum setter para `id`, `createdAt` e `updatedAt`. `@Enumerated(EnumType.STRING)` nos dois enums; `@CreationTimestamp` e `@UpdateTimestamp` nas datas de auditoria. Com `ddl-auto: validate`, um mapeamento errado aqui derruba a inicialização de todo o resto da suíte — é o efeito desejado.

## T-013 — Regra de camada enxergando o Hibernate [pendente]

- Refs: US-005, AC-008
- Arquivos: backend/src/test/java/com/taskmanager/architecture/LayerDependencyTest.java
- Notas: as anotações de timestamp da T-012 são `org.hibernate.annotations`, que a regra atual não verifica — ela só proíbe `org.springframework..` e `jakarta..`. Acrescentar `org.hibernate..` ao conjunto proibido, com exceção nominal de `org.hibernate.annotations..`, pela mesma razão que `jakarta.persistence` é tolerado: é mapeamento. A regra passa a cobrir mais do que cobria; o `@DisplayName` com `@spec:AC-005` não muda, então a prova da 001 continua válida. Único arquivo desta feature que pertencia a outra (T-010 da 001) — a mudança está justificada em "Impacto técnico" da spec.

## T-014 — Porta de persistência no domínio [pendente]

- Refs: US-005, AC-011, AC-013
- Arquivos: backend/src/main/java/com/taskmanager/domain/repository/TaskRepository.java
- Notas: interface Java pura, sem uma linha de Spring (RNF-07). Quatro operações e nenhuma a mais (ASM-009): gravar, buscar por id devolvendo `Optional`, buscar com título e status opcionais, excluir por id. `Optional` em vez de `null` é o que permite à 003 decidir o 404 sem inspecionar referência nula.

## T-015 — Adaptador e repositório Spring Data [pendente]

- Refs: AC-011, AC-012
- Arquivos: backend/src/main/java/com/taskmanager/infrastructure/persistence/TaskJpaRepository.java, backend/src/main/java/com/taskmanager/infrastructure/persistence/TaskRepositoryAdapter.java
- Notas: `TaskJpaRepository` estende `JpaRepository` e carrega a `@Query` JPQL da busca — filtro ausente não restringe, título é comparado em minúsculas com curinga dos dois lados, ordem `createdAt` decrescente (RNF-09, Q-003). `TaskRepositoryAdapter` é um `@Component` que implementa a porta delegando; é o único ponto onde os dois mundos se tocam. Depende de T-014 e T-012.

## T-016 — Prova executável do domínio [pendente]

- Refs: AC-008, AC-009, AC-010, AC-011, AC-012, AC-013
- Arquivos: backend/src/test/java/com/taskmanager/domain/TaskPersistenceTest.java, backend/src/test/java/com/taskmanager/infrastructure/TaskRepositoryPortTest.java
- Notas: as duas classes estendem o `PostgresIntegrationTest` da 001 (ASM-011), contra PostgreSQL real. `TaskPersistenceTest` cobre AC-008 a AC-010 — inclusive lendo a coluna com SQL nativo para provar que o enum foi gravado como texto, e não como ordinal, que é o erro que a AC-009 existe para pegar. `TaskRepositoryPortTest` cobre AC-011 a AC-013 e declara a dependência pelo tipo da **porta**, nunca pelo adaptador: se alguém inverter a direção da dependência, o teste deixa de compilar. Um `@DisplayName` com `@spec:AC-xxx` por critério, senão o gate prova zero.
