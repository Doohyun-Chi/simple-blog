import { useState } from 'react'
import { type Book, reviewSlugOf } from './Book.ts'
import { linkBookReview } from './bookApi.ts'
import { cleanSlug, POST_SLUG_MAX_LENGTH } from './Post.ts'
import { type BookSubmit, useBookSubmit } from './useBookSubmit.ts'

export interface ReviewForm extends BookSubmit {
  slug: string
  /** Keeps only the characters a post slug may have. */
  editSlug: (typed: string) => void
  canLink: boolean
  /** Links the review slug to the book; `onLinked` then gets the slug and the title to start the review post with. */
  link: () => Promise<void>
}

/** 리뷰 적기: the slug of the review post, offered as `review-about-` and the title, for the writer to change. */
export function useReviewForm(book: Book, onLinked: (starter: { title: string; slug: string }) => void): ReviewForm {
  const [slug, updateSlug] = useState(reviewSlugOf(book))
  const submitter = useBookSubmit(() => onLinked({ title: `${book.title} 리뷰`, slug }))
  return {
    ...submitter,
    slug,
    editSlug: (typed) => updateSlug(cleanSlug(typed).slice(0, POST_SLUG_MAX_LENGTH)),
    canLink: slug !== '' && !submitter.busy,
    link: () => submitter.submit(() => linkBookReview(book.id, slug)),
  }
}
