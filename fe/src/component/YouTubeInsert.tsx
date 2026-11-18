import { insertMarkdown$, usePublisher } from '@mdxeditor/editor'
import { type FormEvent, useState } from 'react'
import { registerVideo } from '../model/mediaApi.ts'
import { youTubeMarkdown } from '../model/Media.ts'
import './YouTubeInsert.css'

/**
 * A toolbar button for the editor. It asks for a YouTube link and a title, registers the video with the backend and
 * inserts the Markdown of its thumbnail linking to the video. Nothing is embedded.
 */
export function YouTubeInsert() {
  const insertMarkdown = usePublisher(insertMarkdown$)
  const [open, updateOpen] = useState(false)
  const [url, updateUrl] = useState('')
  const [title, updateTitle] = useState('')
  const [failed, updateFailed] = useState(false)

  async function insert(event: FormEvent) {
    event.preventDefault()
    try {
      insertMarkdown(youTubeMarkdown(title, await registerVideo(url)))
      updateOpen(false)
      updateUrl('')
      updateTitle('')
      updateFailed(false)
    } catch (error: unknown) {
      console.error(error)
      updateFailed(true)
    }
  }

  if (!open) {
    return <button className="youtube-insert-button" type="button" onClick={() => updateOpen(true)}>YouTube</button>
  }
  return (
    <span className="youtube-insert-form" role="group" aria-label="YouTube 영상 삽입">
      <input type="url" placeholder="YouTube 주소" aria-label="YouTube 주소" value={url} onChange={(event) => updateUrl(event.target.value)} />
      <input type="text" placeholder="영상 제목" aria-label="영상 제목" value={title} onChange={(event) => updateTitle(event.target.value)} />
      <button type="button" disabled={url.trim() === ''} onClick={(event) => void insert(event)}>삽입</button>
      <button type="button" onClick={() => updateOpen(false)}>취소</button>
      {failed && <span role="alert" className="youtube-insert-error">YouTube 영상 주소를 확인해 주세요.</span>}
    </span>
  )
}
