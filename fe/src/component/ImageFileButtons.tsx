import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { $isImageNode } from '@mdxeditor/editor'
import { IconReplace, IconTag } from '@tabler/icons-react'
import { $getNodeByKey } from 'lexical'
import { useRef, useState } from 'react'
import { changeFailureText } from '../model/Media.ts'
import { mediaFileUrl, replaceImage } from '../model/mediaApi.ts'
import { ImageSlugRenameDialog } from './ImageSlugRenameDialog.tsx'

interface ImageFileButtonsProps {
  nodeKey: string
  /** The slug of this blog's image the toolbar is over. */
  slug: string
  disabled: boolean
}

/**
 * Changing the stored image behind an image of the editor: its slug, and its file,
 * which is swapped under the same slug. Only the writer who uploaded it may; the backend says no to anyone else.
 */
export function ImageFileButtons({ nodeKey, slug, disabled }: ImageFileButtonsProps) {
  const [editor] = useLexicalComposerContext()
  const picker = useRef<HTMLInputElement>(null)
  const [renaming, updateRenaming] = useState(false)

  function pointTo(source: string) {
    editor.update(() => {
      const node = $getNodeByKey(nodeKey)
      if ($isImageNode(node)) {
        node.setSrc(source)
      }
    })
  }

  async function replaceWith(file: File) {
    try {
      await replaceImage(file, slug)
    } catch (error: unknown) {
      console.error(error)
      window.alert(changeFailureText(error))
    }
  }

  return (
    <>
      <button className="drawio-image-toolbar-button" type="button" title="이미지 slug 변경" aria-label="이미지 slug 변경" disabled={disabled}
        onClick={() => updateRenaming(true)}>
        <IconTag size="1.1em" aria-hidden="true" />
      </button>
      <button className="drawio-image-toolbar-button" type="button" title="이미지 파일 교체" aria-label="이미지 파일 교체" disabled={disabled}
        onClick={() => picker.current?.click()}>
        <IconReplace size="1.1em" aria-hidden="true" />
      </button>
      <input ref={picker} type="file" hidden accept="image/png,image/jpeg,image/gif,image/webp"
        onChange={(event) => {
          const file = event.target.files?.[0]
          event.target.value = ''
          if (file !== undefined) {
            void replaceWith(file)
          }
        }} />
      {renaming && (
        <ImageSlugRenameDialog slug={slug} onClose={() => updateRenaming(false)}
          onRenamed={(media) => { pointTo(mediaFileUrl(media)); updateRenaming(false) }} />
      )}
    </>
  )
}
