import { expect, test } from '@playwright/test'

const user = {
  id: 'user-e2e',
  email: 'e2e@example.com',
  displayName: 'Usuária E2E',
  roles: ['ROLE_USER'],
}

const conversation = {
  id: 'conversation-e2e',
  title: 'Nova conversa',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

test('registers, chats, uploads a document, logs out, and logs in', async ({ page }) => {
  await page.route('**/api/v1/**', async (route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname.replace('/api/v1', '')
    const method = request.method()
    const json = (body: unknown, status = 200) => route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify(body),
    })

    if (path === '/auth/refresh') {
      await json({ message: 'No refresh session.' }, 401)
    } else if (path === '/auth/register' || path === '/auth/login') {
      await json({ accessToken: 'e2e-access-token', tokenType: 'Bearer', expiresAt: '2026-01-01T00:15:00Z', user })
    } else if (path === '/auth/logout') {
      await route.fulfill({ status: 204 })
    } else if (path === '/users/me') {
      await json(user)
    } else if (path === '/users/me/settings') {
      await json({ theme: 'dark', locale: 'pt-BR' })
    } else if (path === '/ai/status') {
      await json({ configured: true })
    } else if (path === '/plans') {
      await json({ currentPlanKey: 'free', plans: [] })
    } else if (path === '/billing') {
      await json({
        planKey: 'free',
        subscriptionStatus: null,
        currentPeriodEnd: null,
        cancellationScheduled: false,
        testModeEnabled: false,
        testSubscription: false,
      })
    } else if (path === '/usage') {
      await json({ totals: { inputTokens: 0, outputTokens: 0, requests: 0 }, recentActivity: [] })
    } else if (path === '/tasks' || path === '/conversations' || path === '/documents') {
      if (method === 'POST' && path === '/conversations') {
        await json(conversation, 201)
      } else if (method === 'POST' && path === '/documents') {
        await json({
          id: 'document-e2e',
          fileName: 'documento-de-teste.txt',
          contentType: 'text/plain',
          sizeBytes: 15,
          createdAt: '2026-01-01T00:00:00Z',
        }, 201)
      } else {
        await json([])
      }
    } else if (path === '/conversations/conversation-e2e/messages/stream') {
      const completion = {
        messages: [
          {
            id: 'user-message-e2e',
            role: 'user',
            content: 'Resuma o propósito do projeto.',
            createdAt: '2026-01-01T00:00:00Z',
          },
          {
            id: 'assistant-message-e2e',
            role: 'assistant',
            content: 'Resposta de teste via SSE.',
            createdAt: '2026-01-01T00:00:01Z',
          },
        ],
      }
      await route.fulfill({
        status: 200,
        contentType: 'text/event-stream',
        body: `event: token\ndata: Resposta de teste via SSE.\n\nevent: complete\ndata: ${JSON.stringify(completion)}\n\n`,
      })
    } else if (path === '/conversations/conversation-e2e/messages') {
      await json([])
    } else {
      await json({})
    }
  })

  await page.goto('/register')
  await page.getByLabel('Nome').fill(user.displayName)
  await page.getByLabel('E-mail').fill(user.email)
  await page.locator('input[autocomplete="new-password"]').fill('senha-e2e-segura-123')
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page.getByRole('link', { name: 'Conversas' })).toBeVisible()

  await page.getByRole('link', { name: 'Conversas' }).click()
  await page.getByLabel('Escreva sua mensagem').fill('Resuma o propósito do projeto.')
  await page.getByRole('button', { name: 'Enviar mensagem' }).click()
  await expect(page.getByText('Resposta de teste via SSE.')).toBeVisible()

  await page.getByRole('link', { name: 'Documentos' }).click()
  await page.locator('input[type="file"]').setInputFiles({
    name: 'documento-de-teste.txt',
    mimeType: 'text/plain',
    buffer: Buffer.from('Conteúdo de teste'),
  })
  await page.getByRole('button', { name: 'Enviar' }).click()
  await expect(page.getByText('documento-de-teste.txt')).toBeVisible()

  await page.getByRole('button', { name: 'Sair da conta' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('E-mail').fill(user.email)
  await page.locator('input[autocomplete="current-password"]').fill('senha-e2e-segura-123')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('link', { name: 'Conversas' })).toBeVisible()
})
