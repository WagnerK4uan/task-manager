# Task Manager

Aplicação de gerenciamento de tarefas: criar, listar, buscar, atualizar,
alterar status e excluir. Backend em Java 21 com Spring Boot, frontend em
Angular, PostgreSQL e execução por Docker.

> **Estado atual:** fundação especificada. O código é construído feature a
> feature seguindo as especificações em [`specs/`](specs/); a primeira,
> [`001-project-setup`](specs/features/001-project-setup/spec.md), está
> aguardando implementação. Esta página ganha as instruções de execução
> (tarefa T-009) quando houver o que executar.

## Tecnologias

Java 21 · Spring Boot · Spring Data JPA · Hibernate · Bean Validation ·
PostgreSQL · Flyway · springdoc-openapi · Angular · TypeScript · Docker ·
JUnit 5 · Mockito · Testcontainers.

## Arquitetura

Separação por camadas com as dependências apontando para dentro — o domínio
não conhece Spring, HTTP nem DTO:

```
presentation  ──▶  application  ──▶  domain  ◀──  infrastructure
```

A persistência entra por uma porta declarada no domínio e implementada em
`infrastructure`, o que mantém o domínio livre do Spring Data e o service
testável sem framework de mock.

O desenho completo, os padrões de código e as decisões arquiteturais estão em
[`CLAUDE.md`](CLAUDE.md).

## Como o projeto é desenvolvido

Cada funcionalidade significativa tem uma especificação antes do código, e a
especificação é auditada mecanicamente contra a implementação:

```bash
node .claude/skills/onp-spec-driven/scripts/onp-spec.mjs status
node .claude/skills/onp-spec-driven/scripts/onp-spec.mjs audit
```

Todo critério de aceite corresponde a um teste anotado; uma feature só é
considerada pronta quando a auditoria sai com código 0.
