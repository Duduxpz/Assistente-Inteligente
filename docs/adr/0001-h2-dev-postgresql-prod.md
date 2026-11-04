# ADR 0001: H2 em desenvolvimento e PostgreSQL em produção

- **Status:** Aceito

## Contexto

O perfil local precisa permitir desenvolvimento e testes sem serviço de banco
externo. Produção requer um banco relacional persistente e suporte a busca vetorial.

## Decisão

Manter H2 no perfil `dev` e PostgreSQL nos perfis padrão/`prod`. Flyway controla
as migrations; Hibernate valida o schema sem atualizá-lo automaticamente. A
migration que depende de pgvector é carregada apenas no perfil de produção.

## Consequências

Desenvolvimento local permanece simples. Alterações no schema PostgreSQL,
especialmente na migration de pgvector, também devem ser validadas em PostgreSQL;
testes com H2 não substituem essa validação.
