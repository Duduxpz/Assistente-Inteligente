# AETHER AI

Plataforma web de assistente inteligente, com backend Java/Spring e frontend
React/TypeScript. O desenvolvimento continua por etapas; a conexão ao
PostgreSQL real fica para a etapa final.

## Implementado até aqui

- Cadastro, login e JWT com senha protegida por PBKDF2.
- Perfil, configurações iniciais e logout no cliente.
- Tarefas com prioridades, datas, edição, conclusão e exclusão.
- Conversas com histórico, renomeação e exclusão.
- Upload, download e exclusão de documentos PDF, TXT e Markdown (limite de 10 MB).
- Área administrativa protegida por função e resumo de utilização.
- Catálogo Free/Plus/Ultra com limites de mensagens, tarefas, conversas e documentos.
- Interface responsiva conectada à API.
- Integração real opcional com OpenRouter via `AIProvider`; sem chave configurada,
  o envio retorna erro explícito e nenhuma resposta fictícia é produzida.
- Preferência de tema escuro, claro ou sincronizado com o sistema, salva na conta.

Recuperação de senha e checkout recorrente via Mercado Pago estão implementados.
No perfil local `dev`, os planos podem ser ativados em modo de teste sem
cobrança; pagamentos reais dependem de credenciais e configuração do Mercado
Pago. Consulte [backend/README.md](./backend/README.md) para os passos.

## Requisitos

- Java 21 e Maven 3.10.0.
- Node.js e npm.
- PostgreSQL para produção (a configuração local atual usa H2 até a etapa final).

## Executar localmente sem PostgreSQL

Abra **dois terminais PowerShell** na raiz do repositório.

No primeiro, gere uma chave JWT temporária e inicie o backend com H2:

```powershell
$bytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_PROFILES_ACTIVE = 'dev'
mvn -f backend\pom.xml spring-boot:run
```

No segundo, inicie o frontend:

```powershell
cd frontend
npm install
npm run dev
```

Abra o endereço impresso pelo Vite, normalmente `http://localhost:5173`, e crie
uma conta. O banco H2 local fica em `backend/data/` e não é enviado ao Git.

## Testes

```powershell
mvn -f backend\pom.xml test
cd frontend
npm run lint
npm run build
```

Consulte [backend/README.md](./backend/README.md) e
[frontend/README.md](./frontend/README.md) para detalhes. Não coloque senhas ou
chaves reais em arquivos versionados.
