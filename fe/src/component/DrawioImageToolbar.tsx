import {
  $isImageNode,
  activeEditor$,
  disableImageSettingsButton$,
  iconComponentFor$,
  openEditImageDialog$,
  parseImageDimension,
  readOnly$,
  useCellValues,
  usePublisher,
} from '@mdxeditor/editor'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { IconVectorBezier2 } from '@tabler/icons-react'
import { $getNodeByKey } from 'lexical'
import { useContext, useState } from 'react'
import { drawioPngOf } from '../model/drawio.ts'
import { mediaFileUrl, uploadImage } from '../model/mediaApi.ts'
import { ReplacedMediaContext } from '../model/ReplacedMedia.ts'
import { useRemote } from '../model/useRemote.ts'
import { DrawioDialog } from './DrawioDialog.tsx'
import './DrawioImageToolbar.css'

/** What MDXEditor passes to the toolbar over an image in the editor. */
interface ImageToolbarProps {
  nodeKey: string
  imageSource: string
  initialImagePath: string | null
  title: string
  alt: string
  width?: number | 'inherit'
  height?: number | 'inherit'
}

/** Removes the image from the post, as MDXEditor's own toolbar does. */
function RemoveImageButton({ nodeKey, disabled }: { nodeKey: string; disabled: boolean }) {
  const [editor] = useLexicalComposerContext()
  const [iconComponentFor] = useCellValues(iconComponentFor$)
  return (
    <button className="drawio-image-toolbar-button" type="button" title="이미지 삭제" aria-label="이미지 삭제" disabled={disabled}
      onClick={(event) => {
        event.preventDefault()
        editor.update(() => $getNodeByKey(nodeKey)?.remove())
      }}>
      {iconComponentFor('delete_small')}
    </button>
  )
}

/** Opens MDXEditor's image settings (source, title, alt text, size) for this image. */
function ImageSettingsButton({ disabled, ...image }: ImageToolbarProps & { disabled: boolean }) {
  const [editor] = useLexicalComposerContext()
  const [iconComponentFor] = useCellValues(iconComponentFor$)
  const openEditImageDialog = usePublisher(openEditImageDialog$)
  const setActiveEditor = usePublisher(activeEditor$)
  const initialValues = { src: image.initialImagePath ?? image.imageSource, title: image.title, altText: image.alt,
    width: parseImageDimension(image.width), height: parseImageDimension(image.height) }
  return (
    <button className="drawio-image-toolbar-button" type="button" title="이미지 설정" aria-label="이미지 설정" disabled={disabled}
      onClick={() => {
        setActiveEditor(editor)
        openEditImageDialog({ nodeKey: image.nodeKey, initialValues })
      }}>
      {iconComponentFor('settings')}
    </button>
  )
}

/**
 * Reopens a draw.io diagram in draw.io. Shown only for an image of this blog that carries one; saving uploads the new
 * PNG and points the image at it, so the post shows the new version. Both versions are reported as replaced, so the
 * next post save deletes whichever of them nothing shows any more.
 */
function EditDrawioButton({ nodeKey, imageSource, disabled }: { nodeKey: string; imageSource: string; disabled: boolean }) {
  const [editor] = useLexicalComposerContext()
  const replaceMedia = useContext(ReplacedMediaContext)
  const png = useRemote(imageSource, (signal) => drawioPngOf(imageSource, signal))
  const [open, updateOpen] = useState(false)
  if (png.state !== 'loaded' || png.value === null) {
    return null
  }
  async function keep(file: File): Promise<boolean> {
    try {
      const source = mediaFileUrl(await uploadImage(file))
      editor.update(() => {
        const node = $getNodeByKey(nodeKey)
        if ($isImageNode(node)) {
          node.setSrc(source)
        }
      })
      replaceMedia([imageSource, source])
      return true
    } catch (error: unknown) {
      console.error(error)
      return false
    }
  }
  return (
    <>
      <button className="drawio-image-toolbar-button" type="button" title="draw.io로 편집" aria-label="draw.io로 편집" disabled={disabled}
        onClick={() => updateOpen(true)}>
        <IconVectorBezier2 size="1.1em" aria-hidden="true" />
      </button>
      {open && <DrawioDialog initialPng={png.value} onSave={keep} onClose={() => updateOpen(false)} />}
    </>
  )
}

/** The toolbar over an image in the editor: MDXEditor's delete and settings buttons, plus draw.io editing for a diagram. */
export function DrawioImageToolbar(props: ImageToolbarProps) {
  const [disableSettings, readOnly] = useCellValues(disableImageSettingsButton$, readOnly$)
  return (
    <div className="drawio-image-toolbar">
      <RemoveImageButton nodeKey={props.nodeKey} disabled={readOnly} />
      {!disableSettings && <ImageSettingsButton {...props} disabled={readOnly} />}
      <EditDrawioButton nodeKey={props.nodeKey} imageSource={props.imageSource} disabled={readOnly} />
    </div>
  )
}
