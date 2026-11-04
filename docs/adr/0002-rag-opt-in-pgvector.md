# ADR 0002: RAG opcional com pgvector

- **Status:** Aceito

## Contexto

Conversas podem usar documentos enviados como contexto, mas indexação semântica
depende de um provedor de embeddings e de armazenamento vetorial.

## Decisão

Manter RAG desativado por padrão e habilitá-lo explicitamente com PostgreSQL e
pgvector. Persistir os chunks com identificador do usuário e filtrar a busca por
esse identificador. Configurar modelo e credenciais do provedor via ambiente.

## Consequências

Instalações sem esses serviços continuam funcionando sem RAG. O schema vetorial
usa dimensões fixas e deve corresponder ao tamanho retornado pelo provedor de
embeddings. O texto recuperado é conteúdo não confiável e não deve ser tratado
como instrução privilegiada.
