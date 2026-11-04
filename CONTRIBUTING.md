# Contribuindo

Obrigado por contribuir com o AETHER AI. Mudanças devem preservar os limites de
acesso por usuário, os limites dos planos e os contratos existentes da API.

## Preparação

- Java 21 e Maven 3.10+ para o backend.
- Node.js e npm para o frontend.
- Docker Compose e PostgreSQL com pgvector para validar mudanças de infraestrutura
  ou RAG.
- Copie os arquivos `.env.example` para configurações locais; nunca inclua tokens,
  senhas ou arquivos `.env` em commits.

## Validar alterações

Na raiz do repositório:

```powershell
mvn -f backend\pom.xml test
cd frontend
npm ci
npm run test:run
npm run test:e2e
npm run lint
npm run build
```

Inclua testes para novos comportamentos e regressões. Alterações em migrations ou
Compose devem ser verificadas com PostgreSQL/pgvector quando disponíveis.

## Pull requests

- Mantenha cada mudança focada e documente alterações de configuração ou API.
- Use commits no padrão Conventional Commits: `feat:`, `fix:`, `test:`,
  `docs:`, `chore:` ou `ci:`.
- Descreva comportamento alterado, testes executados e limitações conhecidas.
- Não inclua dados pessoais, credenciais ou saídas locais de build/testes.
