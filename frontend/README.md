# Frontend

Interface web do AETHER AI, desenvolvida com React, TypeScript, Vite, Tailwind
CSS, React Router e Lucide React.

## Executar localmente

```powershell
npm install
npm run dev
```

O Vite informa o endereço local no terminal, normalmente
`http://localhost:5173`.

## Verificar

```powershell
npm run lint
npm run build
```

O frontend usa a API local em `http://localhost:8080/api/v1` por padrão.
Configure `VITE_API_BASE_URL` em `.env.local` se o backend estiver em outro
endereço. Crie uma conta para acessar conversas, documentos, tarefas e
configurações. A interface de administração só aparece para contas com a função
ADMIN.

O envio de mensagens depende de um provedor real de IA. Sem essa configuração,
a API informa que o serviço está indisponível e não gera respostas simuladas.
