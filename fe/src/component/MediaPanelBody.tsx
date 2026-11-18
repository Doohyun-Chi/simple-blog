import { type ChangeEvent, type DragEvent, useState } from 'react'
import { type Media, uploadFailureText } from '../model/Media.ts'
import { listOfMedias, registerVideo, uploadImage } from '../model/mediaApi.ts'
import { markdownOfMedia } from '../model/mediaMarkdown.ts'
import { useRemote } from '../model/useRemote.ts'
import { MediaGallery } from './MediaGallery.tsx'
import './MediaPanelBody.css'

const VIDEO_FAILURE = 'YouTube 영상 주소를 확인해 주세요.'

interface MediaPanelBodyProps {
  /** Receives the Markdown of the media the writer picked, uploaded or added. */
  onInsert: (markdown: string) => void
}

/**
 * What the media panel shows while it is open: the title that becomes the alt text, an upload of a new image (picked
 * or dropped) or a YouTube link, and the gallery of what is already registered. Whatever is chosen is inserted at
 * once. The gallery is loaded when the panel opens, so it is current every time.
 */
export function MediaPanelBody({ onInsert }: MediaPanelBodyProps) {
  const medias = useRemote('gallery', (signal) => listOfMedias(signal))
  const [title, updateTitle] = useState('')
  const [failure, updateFailure] = useState<string | null>(null)
  const [busy, updateBusy] = useState(false)

  async function insertFrom(source: () => Promise<Media>, failureText: (error: unknown) => string) {
    updateBusy(true)
    try {
      onInsert(markdownOfMedia(await source(), title))
    } catch (error: unknown) {
      console.error(error)
      updateFailure(failureText(error))
      updateBusy(false)
    }
  }

  return (
    <div className="media-panel-body">
      <input
        className="media-panel-field"
        type="text"
        placeholder="제목 (이미지 설명, 영상 제목)"
        aria-label="제목"
        value={title}
        onChange={(event) => updateTitle(event.target.value)}
      />
      <ImageUpload busy={busy} onFile={(file) => void insertFrom(() => uploadImage(file), uploadFailureText)} />
      <VideoLink busy={busy} onLink={(url) => void insertFrom(() => registerVideo(url), () => VIDEO_FAILURE)} />
      {failure !== null && <p className="media-panel-failure" role="alert">{failure}</p>}
      <MediaGallery medias={medias} onPick={(media) => onInsert(markdownOfMedia(media, title))} />
    </div>
  )
}

/** Picking an image file, or dropping one on the area, hands it to `onFile`. */
function ImageUpload({ busy, onFile }: { busy: boolean; onFile: (file: File) => void }) {
  function dropped(event: DragEvent) {
    event.preventDefault()
    const file = event.dataTransfer.files[0]
    if (file !== undefined && !busy) {
      onFile(file)
    }
  }

  function picked(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (file !== undefined) {
      onFile(file)
    }
  }

  return (
    <div className="media-panel-upload" onDragOver={(event) => event.preventDefault()} onDrop={dropped}>
      <label className="media-panel-file">
        새 이미지 올리기
        <input type="file" accept="image/png,image/jpeg,image/gif,image/webp" disabled={busy} onChange={picked} />
      </label>
      <span className="media-panel-hint">또는 이미지 파일을 여기에 놓으세요.</span>
    </div>
  )
}

/** A YouTube address and the button that adds it; Enter in the field adds it too. */
function VideoLink({ busy, onLink }: { busy: boolean; onLink: (url: string) => void }) {
  const [link, updateLink] = useState('')
  const canAdd = !busy && link.trim() !== ''

  return (
    <div className="media-panel-link">
      <input
        className="media-panel-field"
        type="url"
        placeholder="YouTube 주소"
        aria-label="YouTube 주소"
        value={link}
        onChange={(event) => updateLink(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            if (canAdd) {
              onLink(link)
            }
          }
        }}
      />
      <button type="button" disabled={!canAdd} onClick={() => onLink(link)}>영상 추가</button>
    </div>
  )
}
