import { useState } from 'react'
import { type BookInput, type BookPreview, previewFailureText } from './Book.ts'
import { createBook, previewBook } from './bookApi.ts'
import { type BookSubmit, useBookSubmit } from './useBookSubmit.ts'

export interface NewBookForm extends BookSubmit {
  url: string
  editUrl: (url: string) => void
  /** What the page gave, once fetched; null before. */
  fields: BookInput | null
  editFields: (changes: Partial<BookInput>) => void
  /** Why the page could not be read, when it could not. */
  previewFailure: string | null
  fetching: boolean
  fetchPreview: () => Promise<void>
  canRegister: boolean
  register: () => Promise<void>
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

/** The registration of a book (새로운 책): the store URL, what its page gave, the fields the writer edits, and the registering. */
export function useNewBookForm(onRegistered: () => void): NewBookForm {
  const [url, editUrl] = useState('')
  const [fields, updateFields] = useState<BookInput | null>(null)
  const [previewFailure, updatePreviewFailure] = useState<string | null>(null)
  const [fetching, updateFetching] = useState(false)
  const submitter = useBookSubmit(onRegistered)

  async function fetchPreview() {
    const address = url.trim()
    updateFetching(true)
    updatePreviewFailure(null)
    try {
      updateFields(fieldsOf(address, await previewBook(address)))
    } catch (error: unknown) {
      console.error(error)
      updatePreviewFailure(previewFailureText(error))
      updateFields(fieldsOf(address, null))
    } finally {
      updateFetching(false)
    }
  }

  const register = () => submitter.submit(() => createBook(registeredFields(fields)))
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
    register,
  }
}

/** The fields to send: blank optional ones as null. */
function registeredFields(fields: BookInput | null): BookInput {
  if (fields === null) {
    throw new Error('There is nothing to register yet')
  }
  const blankToNull = (text: string | null) => (text === null || text.trim() === '' ? null : text.trim())
  return { ...fields, title: fields.title.trim(), author: blankToNull(fields.author), description: blankToNull(fields.description) }
}
