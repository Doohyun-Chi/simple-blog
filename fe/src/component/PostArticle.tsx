import { dayOf, isClassicContent, type PostBody } from '../model/Post.ts'
import './PostArticle.css'

interface PostArticleProps {
  post: PostBody
}

/**
 * A post or page body: the title as h1, the date, then the content. The content is the author's own stored
 * WordPress markup from the spec's PostBody, so it is inserted as HTML. Classic-editor content keeps its line breaks.
 */
export function PostArticle({ post }: PostArticleProps) {
  return (
    <article className="post-article">
      <h1 className="post-article-title">{post.title}</h1>
      <time className="post-article-date" dateTime={post.date}>{dayOf(post.date)}</time>
      <div
        className={isClassicContent(post.content) ? 'post-article-content post-article-content-classic' : 'post-article-content'}
        dangerouslySetInnerHTML={{ __html: post.content }}
      />
    </article>
  )
}
