import { IconArticle } from '@tabler/icons-react'
import { Card, Icon } from 'design-system'
import { dayOf, type PostSummary, PostType, readerSlug, statusLabelOf } from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import './PostSummaryCards.css'

const EXCERPT_LINES = 3

function metaOf(post: PostSummary): string {
  const status = statusLabelOf(post.status)
  return status === null ? dayOf(post.date) : `${status} · ${dayOf(post.date)}`
}

/** The posts as cards, two to a row: the first image of the post, its title, its date and the start of its text. */
export function PostSummaryCards({ posts }: { posts: PostSummary[] }) {
  return (
    <ol className="post-summary-cards">
      {posts.map((post) => (
        <Card key={post.id} as="li" className="post-summary-card" title={post.title} titleAs="h3" mediaWidth="8em"
          href={contentPath(PostType.POST, readerSlug(post.slug))} meta={metaOf(post)} lines={EXCERPT_LINES}
          {...(post.thumbnail === undefined
            ? { media: <Icon className="post-summary-card-none" icon={IconArticle} size="3em" /> }
            : { image: { src: post.thumbnail, alt: '' } })}>
          {post.excerpt}
        </Card>
      ))}
    </ol>
  )
}
