import { Children, isValidElement, useState, type ComponentProps, type ReactNode } from 'react'
import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import remarkGfm from 'remark-gfm'
import 'highlight.js/styles/github-dark.css'

function markdownText(children: ReactNode): string {
  return Children.toArray(children).map((child) => {
    if (typeof child === 'string' || typeof child === 'number') return String(child)
    if (isValidElement<{ children?: ReactNode }>(child)) return markdownText(child.props.children)
    return ''
  }).join('')
}

function MarkdownCodeBlock({ children }: ComponentProps<'pre'>) {
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(markdownText(children))
      setCopied(true)
      setError('')
    } catch {
      setError('Não foi possível copiar o código para a área de transferência.')
    }
  }

  return (
    <div className="code-block">
      <button className="code-copy-button" type="button" onClick={() => void copyCode()}>
        {copied ? 'Copiado' : 'Copiar código'}
      </button>
      <pre>{children}</pre>
      {error && <span className="code-copy-error" role="status">{error}</span>}
    </div>
  )
}

export function MarkdownContent({ content }: { content: string }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      rehypePlugins={[rehypeHighlight]}
      components={{ pre: MarkdownCodeBlock }}
    >
      {content}
    </ReactMarkdown>
  )
}
