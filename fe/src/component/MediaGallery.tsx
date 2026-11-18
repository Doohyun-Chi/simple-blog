import { isVideo, type Media } from '../model/Media.ts'
import { tilePictureUrl } from '../model/mediaMarkdown.ts'
import type { Remote } from '../model/useRemote.ts'
import './MediaGallery.css'

interface MediaGalleryProps {
  medias: Remote<Media[]>
  onPick: (media: Media) => void
}

/** The registered media as tiles, newest first; picking one hands it to `onPick`. */
export function MediaGallery({ medias, onPick }: MediaGalleryProps) {
  switch (medias.state) {
    case 'loading':
      return <p className="media-gallery-note">불러오는 중…</p>
    case 'failed':
      return <p className="media-gallery-note" role="alert">미디어를 불러오지 못했습니다.</p>
    case 'loaded':
      return medias.value.length === 0
        ? <p className="media-gallery-note">아직 올린 미디어가 없습니다.</p>
        : <Tiles medias={medias.value} onPick={onPick} />
  }
}

function Tiles({ medias, onPick }: { medias: Media[]; onPick: (media: Media) => void }) {
  return (
    <ul className="media-gallery">
      {medias.map((media) => (
        <li key={media.slug}>
          <button className="media-gallery-tile" type="button" title={media.slug} onClick={() => onPick(media)}>
            <img src={tilePictureUrl(media)} alt={media.slug} loading="lazy" />
            {isVideo(media) && <span className="media-gallery-badge" aria-label="YouTube">▶</span>}
          </button>
        </li>
      ))}
    </ul>
  )
}
