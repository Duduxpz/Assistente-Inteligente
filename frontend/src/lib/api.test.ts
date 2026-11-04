import { afterEach, describe, expect, it, vi } from 'vitest'
import { streamChatMessage } from './api'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('streamChatMessage', () => {
  it('parses token events split across network chunks and returns completion messages', async () => {
    const completion = {
      messages: [
        {
          id: 'message-1',
          role: 'assistant',
          content: 'Olá!',
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
    }
    const payload = new TextEncoder().encode(
      `event: token\ndata: Olá\n\nevent: token\ndata: !\n\nevent: complete\ndata: ${JSON.stringify(completion)}\n\n`,
    )
    const responseStream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(payload.slice(0, 29))
        controller.enqueue(payload.slice(29))
        controller.close()
      },
    })
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(responseStream))
    const onToken = vi.fn()

    const result = await streamChatMessage(
      '/conversations/conversation-1/messages/stream',
      'Oi',
      'test-token',
      new AbortController().signal,
      onToken,
    )

    expect(onToken.mock.calls).toEqual([['Olá'], ['!']])
    expect(result).toEqual(completion)
  })
})
