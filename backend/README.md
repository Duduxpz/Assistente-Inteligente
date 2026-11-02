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
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
mvn -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Para produção, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET`
com valores gerenciados fora do repositório, e não ative o perfil `dev`.
`JWT_SECRET` precisa ser uma chave Base64 com pelo menos 32 bytes aleatórios.

O Flyway executa as migrations ao iniciar. O Hibernate valida o schema sem
alterá-lo. Nunca use credenciais reais em arquivos versionados.

## Endpoints implementados

- `POST /api/v1/auth/register` e `POST /api/v1/auth/login`: cadastro e login.
- `GET /api/v1/users/me`: perfil da conta autenticada.
- `GET`, `POST`, `PUT` e `DELETE /api/v1/tasks`: tarefas restritas ao usuário autenticado.
- `GET`, `POST`, `PUT` e `DELETE /api/v1/conversations`: conversas do usuário.
- `GET /api/v1/conversations/{id}/messages` e `POST .../messages`: histórico e envio.

O token de acesso expira em 15 minutos por padrão. O cliente pode remover o token
no logout; a invalidação imediata de tokens e refresh tokens ainda não foram
implementados. Senhas são armazenadas com PBKDF2. A integração de IA usa a
interface `AIProvider`; sem um provedor configurado, o envio de mensagens retorna
HTTP 503 e não grava uma resposta simulada.

## Verificação

Na raiz do repositório, execute:

```powershell
mvn -f backend\pom.xml test
```

Os testes usam H2 em memória, aplicam as migrations e verificam autenticação,
propriedade de dados, tarefas e ausência de respostas fictícias. O perfil normal
continua configurado para PostgreSQL; o perfil `dev` usa H2 local.
