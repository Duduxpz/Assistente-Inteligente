import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MarkdownContent } from './MarkdownContent'

describe('MarkdownContent', () => {
  it('highlights code and copies its plain text', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText },
    })
    const { container } = render(
      <MarkdownContent content={'```javascript\nconst answer = 42\n```'} />,
    )

    expect(container.querySelector('code.hljs')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Copiar código' }))

    expect(writeText).toHaveBeenCalledWith('const answer = 42\n')
    expect(await screen.findByRole('button', { name: 'Copiado' })).toBeInTheDocument()
  })
})
