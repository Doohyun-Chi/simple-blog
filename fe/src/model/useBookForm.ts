import { useState } from 'react'
import { type Book, type BookInput, type BookPreview, previewFailureText } from './Book.ts'
import { createBook, deleteBook, previewBook, updateBook } from './bookApi.ts'
import { type BookSubmit, useBookSubmit } from './useBookSubmit.ts'

export interface BookForm extends BookSubmit {
  url: string
  editUrl: (url: string) => void
  /** What the page gave (or what the book already has), once there is any; null before. */
  fields: BookInput | null
  editFields: (changes: Partial<BookInput>) => void
  /** Why the page could not be read, when it could not. */
  previewFailure: string | null
  fetching: boolean
  fetchPreview: () => Promise<void>
  canRegister: boolean
  /** Registers a new book, or saves the changes of the one being edited. */
  register: () => Promise<void>
  /** 삭제: removes the book being edited from the shelf, once `confirmed` says yes. */
  remove: (confirmed: () => boolean) => Promise<void>
}

function fieldsOf(url: string, preview: BookPreview | null): BookInput {
  return {
    title: preview?.title ?? '',
    author: preview?.author ?? null,
    bookstoreUrl: url,
    thumbnailUrl: preview?.thumbnailUrl ?? null,
    description: preview?.description ?? null,
  }
}

function fieldsOfBook(book: Book | null): BookInput | null {
  return book === null ? null : {
    title: book.title,
    author: book.author,
    bookstoreUrl: book.bookstoreUrl,
    thumbnailUrl: book.thumbnailUrl,
    description: book.description,
  }
}

/**
 * The form of a book: the store URL, what its page gave, the fields the writer edits, and the saving. With a `book` it edits that
 * book (its fields are shown from the start and it can be removed); without one it registers a new book.
 */
export function useBookForm(book: Book | null, onDone: () => void): BookForm {
  const [url, editUrl] = useState(book?.bookstoreUrl ?? '')
  const [fields, updateFields] = useState<BookInput | null>(fieldsOfBook(book))
  const [previewFailure, updatePreviewFailure] = useState<string | null>(null)
  const [fetching, updateFetching] = useState(false)
  const submitter = useBookSubmit(onDone)

  async function fetchPreview() {
    const address = url.trim()
    updateFetching(true)
    updatePreviewFailure(null)
    try {
      updateFields(fieldsOf(address, await previewBook(address)))
    } catch (error: unknown) {
      console.error(error)
      updatePreviewFailure(previewFailureText(error))
      updateFields((current) => ({ ...(current ?? fieldsOf(address, null)), bookstoreUrl: address }))
    } finally {
      updateFetching(false)
    }
  }

  const input = savedFields(fields, url)
  return {
    ...submitter,
    url,
    editUrl,
    fields,
    editFields: (changes) => updateFields((current) => (current === null ? current : { ...current, ...changes })),
    previewFailure,
    fetching,
    fetchPreview,
    canRegister: fields !== null && fields.title.trim() !== '' && !submitter.busy,
    register: () => submitter.submit(() => (book === null ? createBook(input()) : updateBook(book.id, input()))),
    remove: async (confirmed) => {
      if (book !== null && confirmed()) {
        await submitter.submit(() => deleteBook(book.id))
      }
    },
  }
}

/** The fields to send, under the URL as it stands: blank optional ones as null. */
function savedFields(fields: BookInput | null, url: string): () => BookInput {
  return () => {
    if (fields === null) {
      throw new Error('There is nothing to save yet')
    }
    const blankToNull = (text: string | null) => (text === null || text.trim() === '' ? null : text.trim())
    return {
      title: fields.title.trim(),
      author: blankToNull(fields.author),
      bookstoreUrl: url.trim(),
      thumbnailUrl: blankToNull(fields.thumbnailUrl),
      description: blankToNull(fields.description),
    }
  }
}
