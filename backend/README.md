# Backend

API REST em Java 21 com Spring Boot 4.1.2 e Maven.

## Dependências

- Spring Web MVC: endpoints REST.
- Spring Security Resource Server: autenticação stateless por JWT e validação dos tokens.
- Spring Data JPA: camada de persistência.
- PostgreSQL e Flyway: banco de dados e migrations versionadas.
- Jakarta Validation: validação de entradas das próximas etapas.
- Springdoc OpenAPI: documentação Swagger em `/swagger-ui.html`, desativada no perfil `prod`.
- Actuator: endpoints `/actuator/health` e `/actuator/info`; detalhes operacionais exigem autenticação.
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

O perfil `prod` configura PostgreSQL, valida o schema migrado e desativa a documentação
OpenAPI. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` com valores
gerenciados fora do repositório, e não ative o perfil `dev`.
`JWT_SECRET` precisa ser uma chave Base64 com pelo menos 32 bytes aleatórios.

O Flyway executa as migrations ao iniciar. O Hibernate valida o schema sem
alterá-lo. Nunca use credenciais reais em arquivos versionados.

Para executar a stack de produção localmente com PostgreSQL, consulte a seção
Docker Compose no [README principal](../README.md). O healthcheck do backend usa
`/actuator/health`; logs da aplicação são emitidos em JSON para stdout.

## Endpoints implementados

- `POST /api/v1/auth/register` e `POST /api/v1/auth/login`: cadastro e login.
- `GET /api/v1/auth/password-reset/status`: verifica se o envio de e-mail está configurado.
- `POST /api/v1/auth/password-reset/request` e `/complete`: recuperação de senha
  com token temporário de uso único.
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
ativas, conversas e armazenamento total de documentos. A cobrança recorrente
usa Mercado Pago; no perfil `dev`, os botões ativam uma assinatura de teste sem
cobrança, válida por 30 dias e sem nenhuma rota equivalente em produção.

Para deixar uma conta Ultra no banco H2 local, informe o e-mail cadastrado na
variável `AETHER_TEST_ULTRA_EMAIL` em `backend/.env` e reinicie o backend com o
perfil `dev`. O modo de teste exige o perfil `dev` sem o perfil `prod` e suas
rotas não são registradas em produção.

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

### Recuperação de senha com Resend

1. Crie uma conta no [Resend](https://resend.com/) e cadastre/verifique um domínio
   que você controla. O Resend não envia mensagens de produção de um domínio não
   verificado.
2. Gere uma API key no painel do Resend.
3. Configure localmente em `backend/.env`:

   ```dotenv
   EMAIL_PROVIDER=resend
   RESEND_API_KEY=sua-chave-local
   RESEND_FROM=AETHER AI <conta@seu-dominio.com>
   FRONTEND_BASE_URL=http://localhost:5173
   ```

4. Reinicie o backend. A tela **Esqueci minha senha** só oferece o formulário
   de envio quando o provedor está configurado.

O token de recuperação é aleatório, armazenado somente como hash, expira em 30
minutos e pode ser usado uma vez. Redefinir a senha revoga imediatamente os
tokens de acesso já emitidos. A API não revela se o e-mail informado pertence
a uma conta.

### Assinaturas com Mercado Pago

A cobrança mensal está integrada ao endpoint oficial de assinaturas (`/preapproval`).
O plano da conta só é atualizado após um webhook com assinatura HMAC válida e
uma consulta servidor-servidor ao Mercado Pago. O retorno do navegador nunca
ativa o plano. O cancelamento interrompe a renovação, preservando o acesso até
o fim do período informado pelo provedor.

1. Crie uma aplicação em [Suas integrações do Mercado Pago](https://www.mercadopago.com.br/developers/panel/app)
   e use primeiro as credenciais de teste.
2. Configure no arquivo local `backend/.env` (não compartilhe esses valores):

   ```dotenv
   MERCADOPAGO_ACCESS_TOKEN=seu-access-token-de-teste
   MERCADOPAGO_WEBHOOK_SECRET=sua-chave-secreta-de-webhook
   FRONTEND_BASE_URL=http://localhost:5173
   ```

3. Publique o backend em uma URL HTTPS acessível pelo Mercado Pago. Cadastre
   `https://seu-dominio/api/v1/billing/webhooks/mercadopago` como URL de webhook
   e habilite o evento **subscription_preapproval**.
4. Copie a assinatura secreta gerada para `MERCADOPAGO_WEBHOOK_SECRET` e
   reinicie o backend.
5. Teste checkout, confirmação, cancelamento e expiração com contas e cartões
   de teste antes de configurar credenciais de produção.

As opções de assinatura dos planos Plus e Ultra criam cobrança recorrente
mensal em BRL. Para reduzir a possibilidade de duplicidade, uma conta não pode
abrir outra assinatura enquanto houver pagamento pendente ou período pago
ativo. A expiração é aplicada ao consultar o plano ou utilizar recursos
limitados; a infraestrutura atual não executa tarefas agendadas.

Sem credenciais, checkout retorna erro explícito e não simula pagamento. Não
coloque tokens do Mercado Pago, chaves de webhook ou credenciais de teste no
Git nem envie esses segredos pelo chat.

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
