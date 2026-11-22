import type { ComponentProps } from 'react'
import Markdown, { type ExtraProps } from 'react-markdown'
import rehypeRaw from 'rehype-raw'
import rehypeSanitize from 'rehype-sanitize'
import { mermaidSourceOf } from '../model/mermaid.ts'
import { dayOf, isClassicContent, type PostBody } from '../model/Post.ts'
import { AdSlot } from './AdSlot.tsx'
import { HashTagList } from './HashTagList.tsx'
import { MermaidDiagram } from './MermaidDiagram.tsx'
import { ShareLinks } from './ShareLinks.tsx'
import './PostArticle.css'

interface PostArticleProps {
  post: PostBody
  /** Where the 편집하기 link goes; without it the link is not shown. */
  editHref?: string
  /** The link the share buttons copy and post to LinkedIn; without it they are not shown. */
  shareHref?: string
}

/** A code block as written, or the diagram it describes when it is a ```mermaid block. */
function ArticleCodeBlock({ node, ...props }: ComponentProps<'pre'> & ExtraProps) {
  const source = mermaidSourceOf(node)
  return source === null ? <pre {...props} /> : <MermaidDiagram source={source} />
}

/** The renderer's elements this article replaces. */
const ARTICLE_COMPONENTS = { pre: ArticleCodeBlock }

/**
 * A post or page body: the title as h1, the date, then the content. The content is Markdown, as the editor writes it
 * (images and YouTube thumbnails included). Posts migrated from WordPress still hold HTML, so raw HTML is kept, but
 * sanitized: scripts, event handlers and the like never reach the page. A ```mermaid block is drawn as a diagram after
 * sanitizing. Classic-editor content keeps its line breaks.
 */
export function PostArticle({ post, editHref, shareHref }: PostArticleProps) {
  return (
    <article className="post-article">
      <h1 className="post-article-title">{post.title}</h1>
      <div className="post-article-meta">
        <time className="post-article-date" dateTime={post.date}>{dayOf(post.date)}</time>
        {shareHref !== undefined && <ShareLinks url={shareHref} />}
        {editHref !== undefined && <a className="post-article-edit" href={editHref}>편집하기</a>}
      </div>
      <div
        className={isClassicContent(post.content) ? 'post-article-content post-article-content-classic' : 'post-article-content'}
      >
        <Markdown rehypePlugins={[rehypeRaw, rehypeSanitize]} components={ARTICLE_COMPONENTS}>{post.content}</Markdown>
      </div>
      <HashTagList hashTags={post.hashTags} />
      <AdSlot position="post" />
    </article>
  )
}
