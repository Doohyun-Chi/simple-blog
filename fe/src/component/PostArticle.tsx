import Markdown from 'react-markdown'
import rehypeRaw from 'rehype-raw'
import rehypeSanitize from 'rehype-sanitize'
import { dayOf, isClassicContent, type PostBody } from '../model/Post.ts'
import { AdSlot } from './AdSlot.tsx'
import './PostArticle.css'

interface PostArticleProps {
  post: PostBody
  /** Where the 편집하기 link goes; without it the link is not shown. */
  editHref?: string
}

/**
 * A post or page body: the title as h1, the date, then the content. The content is Markdown, as the editor writes it
 * (images and YouTube thumbnails included). Posts migrated from WordPress still hold HTML, so raw HTML is kept, but
 * sanitized: scripts, event handlers and the like never reach the page. Classic-editor content keeps its line breaks.
 */
export function PostArticle({ post, editHref }: PostArticleProps) {
  return (
    <article className="post-article">
      <h1 className="post-article-title">{post.title}</h1>
      <div className="post-article-meta">
        <time className="post-article-date" dateTime={post.date}>{dayOf(post.date)}</time>
        {editHref !== undefined && <a className="post-article-edit" href={editHref}>편집하기</a>}
      </div>
      <AdSlot position="top" />
      <div
        className={isClassicContent(post.content) ? 'post-article-content post-article-content-classic' : 'post-article-content'}
      >
        <Markdown rehypePlugins={[rehypeRaw, rehypeSanitize]}>{post.content}</Markdown>
      </div>
      <AdSlot position="bottom" />
    </article>
  )
}
