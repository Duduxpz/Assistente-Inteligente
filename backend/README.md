# Backend

API REST em Java 21 com Spring Boot 4.1.2 e Maven.

## Dependências

- Spring Web MVC: endpoints REST.
- Spring Security: padrão stateless; somente health check e documentação são públicos nesta etapa.
- Spring Data JPA: camada de persistência.
- PostgreSQL e Flyway: banco de dados e migrations versionadas.
- Jakarta Validation: validação de entradas das próximas etapas.
- Springdoc OpenAPI: documentação Swagger em `/swagger-ui.html`.
- Actuator: endpoint operacional `/actuator/health`.
- JUnit, Mockito e H2 (apenas testes): testes automatizados sem exigir um servidor PostgreSQL local.

## Configuração local

O PostgreSQL precisa estar instalado e em execução. Crie o banco `aether_ai` e
configure as variáveis de ambiente antes de iniciar a aplicação. O arquivo
`.env.example` documenta os nomes esperados e não contém uma senha válida. A
aplicação carrega, quando existe, um arquivo local `backend/.env` no formato
`CHAVE=valor`; esse arquivo é ignorado pelo Git.

Exemplo no PowerShell, substituindo os valores pelos da sua instalação local:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/aether_ai'
$env:DB_USERNAME = 'aether_ai'
$env:DB_PASSWORD = '<sua-senha-local>'
mvn spring-boot:run
```

Como alternativa, defina `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` em
`backend/.env` para carregá-los automaticamente ao iniciar a aplicação.

O Flyway executa as migrations ao iniciar. O Hibernate valida o schema sem
alterá-lo. Não use credenciais reais em arquivos versionados.

## Verificação

Na raiz do repositório, execute:

```powershell
mvn -f backend\pom.xml test
```

O teste usa H2 somente em memória, aplica a migration inicial e verifica o
health check e a proteção de rotas. A aplicação real continua usando PostgreSQL.
