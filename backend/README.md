# Backend

API REST em Java 21 com Spring Boot 4.1.2 e Maven.

## Dependências

- Spring Web MVC: endpoints REST.
- Spring Security Resource Server: autenticação stateless por JWT e validação dos tokens.
- Spring Data JPA: camada de persistência.
- PostgreSQL e Flyway: banco de dados e migrations versionadas.
- Jakarta Validation: validação de entradas das próximas etapas.
- Springdoc OpenAPI: documentação Swagger em `/swagger-ui.html`.
- Actuator: endpoint operacional `/actuator/health`.
- JUnit e H2: testes automatizados e perfil de desenvolvimento sem exigir um servidor PostgreSQL local.

## Configuração local

O arquivo `.env.example` documenta as variáveis necessárias; ele não contém
credenciais válidas. A aplicação carrega, quando existe, um arquivo local
`backend/.env` no formato `CHAVE=valor`; esse arquivo é ignorado pelo Git.

Para desenvolver sem PostgreSQL, gere uma chave JWT local aleatória e inicie o
perfil H2. O banco H2 fica em `backend/data/`, fora do Git:

```powershell
$bytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn -f backend\pom.xml spring-boot:run
```

Para produção, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET`
com valores gerenciados fora do repositório, e não ative o perfil `dev`.
`JWT_SECRET` precisa ser uma chave Base64 com pelo menos 32 bytes aleatórios.

O Flyway executa as migrations ao iniciar. O Hibernate valida o schema sem
alterá-lo. Nunca use credenciais reais em arquivos versionados.

## Endpoints implementados

- `POST /api/v1/auth/register` e `POST /api/v1/auth/login`: cadastro e login.
- `GET` e `PUT /api/v1/users/me`: perfil da conta autenticada.
- `GET` e `PUT /api/v1/users/me/settings`: preferências da conta.
- `GET`, `POST`, `PUT` e `DELETE /api/v1/tasks`: tarefas restritas ao usuário autenticado.
- `GET`, `POST`, `PUT` e `DELETE /api/v1/conversations`: conversas do usuário.
- `GET /api/v1/conversations/{id}/messages` e `POST .../messages`: histórico e envio.
- `GET`, `POST` e `DELETE /api/v1/documents` e `GET .../{id}/download`: arquivos
  privados por usuário, PDF/TXT/Markdown até 10 MB.
- `GET /api/v1/usage`: resumo de uso da própria conta.
- `GET /api/v1/plans`: catálogo de preços e limites do plano atual.
- `GET /api/v1/admin/users` e `/api/v1/admin/usage`: endpoints com função ADMIN.

O catálogo mensal atual é Free (R$ 0), Plus (R$ 25) e Ultra (R$ 100). Os limites
por plano são aplicados pela API: mensagens de IA concluídas por mês, tarefas
ativas, conversas e armazenamento total de documentos. A cobrança e a troca de
plano ainda não estão conectadas a um gateway de pagamento.

O token de acesso expira em 15 minutos por padrão. O cliente pode remover o token
no logout; a invalidação imediata de tokens e refresh tokens ainda não foram
implementados. Senhas são armazenadas com PBKDF2. A integração de IA usa a
interface `AIProvider`; sem um provedor configurado, o envio de mensagens retorna
HTTP 503 e não grava uma resposta simulada.

### Conectar ao OpenRouter

1. Crie uma conta em [openrouter.ai](https://openrouter.ai/), gere uma chave em
   **Keys** e adicione créditos à conta. A chave é secreta: não a envie por chat
   nem a coloque em arquivos versionados.
2. Crie `backend/.env` a partir de `.env.example` e preencha localmente:

   ```dotenv
   AI_PROVIDER=openrouter
   OPENROUTER_API_KEY=sua-chave-local
   OPENROUTER_MODEL=openai/gpt-4o-mini
   OPENROUTER_SITE_URL=http://localhost:5173
   ```

3. Reinicie o backend. Para voltar a desativar a integração, altere
   `AI_PROVIDER=none`.

O modelo pode ser trocado por outro identificador disponível na conta do
OpenRouter. O AETHER registra modelo e tokens somente após uma resposta válida;
falhas do provedor não são salvas como mensagens.

O perfil de administrador não pode ser atribuído pelo cadastro público; a
concessão inicial exige uma operação administrativa controlada no banco. A
desativação de uma conta é verificada nas requisições autenticadas.

## Verificação

Na raiz do repositório, execute:

```powershell
mvn -f backend\pom.xml test
```

Os testes usam H2 em memória, aplicam as migrations e verificam autenticação,
autorização, propriedade de dados, tarefas, arquivos e ausência de respostas
fictícias. O perfil normal continua configurado para PostgreSQL; o perfil `dev`
usa H2 local.
