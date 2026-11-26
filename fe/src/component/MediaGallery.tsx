import { IconArrowBarToDown, IconTrash } from '@tabler/icons-react'
import { isVideo, type Media } from '../model/Media.ts'
import { tilePictureUrl } from '../model/mediaMarkdown.ts'
import type { Remote } from '../model/useRemote.ts'
import './MediaGallery.css'

interface MediaGalleryProps {
  medias: Remote<Media[]>
  /** The slug of the one selected tile, if any. */
  selected: string | null
  onSelect: (slug: string) => void
  /** Inserts the media: the insert icon of the selected tile, or a double click on a tile. */
  onInsert: (media: Media) => void
  onRemove: (media: Media) => void
}

/** The registered media as tiles, newest first; one can be selected, and then it can be inserted or deleted. */
export function MediaGallery({ medias, ...actions }: MediaGalleryProps) {
  switch (medias.state) {
    case 'loading':
      return <p className="media-gallery-note">불러오는 중…</p>
    case 'failed':
      return <p className="media-gallery-note" role="alert">미디어를 불러오지 못했습니다.</p>
    case 'loaded':
      return medias.value.length === 0
        ? <p className="media-gallery-note">아직 올린 미디어가 없습니다.</p>
        : <Tiles medias={medias.value} {...actions} />
  }
}

type TilesProps = Omit<MediaGalleryProps, 'medias'> & { medias: Media[] }

function Tiles({ medias, selected, onSelect, onInsert, onRemove }: TilesProps) {
  return (
    <ul className="media-gallery">
      {medias.map((media) => (
        <li key={media.slug} className="media-gallery-item">
          <button className="media-gallery-tile" type="button" title={media.slug} aria-pressed={media.slug === selected}
            onClick={() => onSelect(media.slug)} onDoubleClick={() => onInsert(media)}>
            <img src={tilePictureUrl(media)} alt={media.slug} loading="lazy" />
            {isVideo(media) && <span className="media-gallery-badge" aria-label="YouTube">▶</span>}
          </button>
          {media.slug === selected && (
            <div className="media-gallery-actions">
              <button type="button" aria-label="본문에 넣기" title="본문에 넣기" onClick={() => onInsert(media)}>
                <IconArrowBarToDown size="1.1em" aria-hidden="true" />
              </button>
              <button type="button" className="media-gallery-remove" aria-label="삭제" title="삭제" onClick={() => onRemove(media)}>
                <IconTrash size="1.1em" aria-hidden="true" />
              </button>
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}
