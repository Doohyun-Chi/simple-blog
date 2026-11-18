import {
  BlockTypeSelect,
  BoldItalicUnderlineToggles,
  CodeToggle,
  CreateLink,
  InsertCodeBlock,
  ListsToggle,
  MDXEditor,
  UndoRedo,
  codeBlockPlugin,
  codeMirrorPlugin,
  diffSourcePlugin,
  headingsPlugin,
  imagePlugin,
  linkDialogPlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  thematicBreakPlugin,
  toolbarPlugin,
} from '@mdxeditor/editor'
import '@mdxeditor/editor/style.css'
import { type ReactNode, useMemo, useState } from 'react'
import {
  cleanSlug,
  isSavable,
  POST_SLUG_MAX_LENGTH,
  POST_TITLE_MAX_LENGTH,
  type PostInput,
} from '../model/Post.ts'
import { FullScreenContext } from '../model/FullScreen.ts'
import { uploadFailureText } from '../model/Media.ts'
import { mediaFileUrl, uploadImage } from '../model/mediaApi.ts'
import { contentPath } from '../model/route.ts'
import type { SaveFailure, SaveState } from '../model/useSavePost.ts'
import { FullScreenToggle } from './FullScreenToggle.tsx'
import { ViewModeToggle } from './ViewModeToggle.tsx'
import { MediaPanel } from './MediaPanel.tsx'
import './PostEditor.css'

const CODE_LANGUAGES = {
  '': '일반 텍스트',
  java: 'Java',
  ts: 'TypeScript',
  tsx: 'TSX',
  css: 'CSS',
  sql: 'SQL',
  sh: 'Shell',
  json: 'JSON',
  yaml: 'YAML',
}

/**
 * Plugins for the body editor: Markdown formatting, links, images and code blocks, and a toolbar for them. An image
 * picked in the toolbar, dropped on the editor or pasted is sent to the backend; `onImageFailed` hears why one was not.
 */
function editorPlugins(onImageFailed: (error: unknown) => void) {
  async function uploadedImageUrl(file: File): Promise<string> {
    try {
      return mediaFileUrl(await uploadImage(file))
    } catch (error: unknown) {
      console.error(error)
      onImageFailed(error)
      throw error
    }
  }

  return [
    headingsPlugin(),
    listsPlugin(),
    quotePlugin(),
    thematicBreakPlugin(),
    linkPlugin(),
    linkDialogPlugin(),
    imagePlugin({ imageUploadHandler: uploadedImageUrl }),
    codeBlockPlugin({ defaultCodeBlockLanguage: '' }),
    codeMirrorPlugin({ codeBlockLanguages: CODE_LANGUAGES }),
    diffSourcePlugin({ viewMode: 'rich-text' }),
    markdownShortcutPlugin(),
    toolbarPlugin({
      toolbarContents: () => (
        <>
          <ViewModeToggle>
            <UndoRedo />
            <BlockTypeSelect />
            <BoldItalicUnderlineToggles />
            <CodeToggle />
            <ListsToggle />
            <CreateLink />
            <MediaPanel />
            <InsertCodeBlock />
          </ViewModeToggle>
          <FullScreenToggle />
        </>
      ),
    }),
  ]
}

const SAVE_FAILURE_TEXT: Record<SaveFailure, string> = {
  invalid: '제목과 Slug를 확인해 주세요.',
  duplicate: '이미 사용 중인 Slug입니다.',
  forbidden: '저장할 권한이 없습니다.',
  missing: '저장된 글을 찾을 수 없습니다. 다시 저장하면 새 글로 저장합니다.',
  failed: '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
}

interface PostEditorProps {
  /** Where the last save stands. */
  saveState: SaveState
  /** Saves the post: the first call creates a draft, later calls update it. */
  onSave: (input: PostInput) => void
  /** Publishes the post. While it is missing, the 공개 button stays disabled. */
  onPublish?: () => void
}

/** The line under a field. */
function FieldHint({ note }: { note: ReactNode }) {
  return (
    <div className="post-editor-hint">
      <span className="post-editor-note" role="status">{note}</span>
    </div>
  )
}

function saveNote(state: SaveState): ReactNode {
  switch (state.status) {
    case 'idle':
      return null
    case 'saving':
      return '저장 중…'
    case 'saved':
      return <>초안으로 저장했습니다. <a href={contentPath('post', state.slug)}>보기</a></>
    case 'failed':
      return SAVE_FAILURE_TEXT[state.failure]
  }
}

/**
 * The writing area from fe/DESIGN.md: the title with the save and publish buttons beside it, the slug, the MDX body
 * filling the rest, and the hashtags on the last line. The title and slug limits appear as placeholders only; the
 * backend refuses longer ones. The slug is limited to the characters the spec allows as it is typed. 공개 stays
 * disabled: the spec has no publish operation, and hashtags have no field in it, so they are not sent.
 */
export function PostEditor({ saveState, onSave, onPublish }: PostEditorProps) {
  const [title, updateTitle] = useState('')
  const [slug, updateSlug] = useState('')
  const [markdown, updateMarkdown] = useState('')
  const [fullScreen, updateFullScreen] = useState(false)
  const [imageNote, updateImageNote] = useState<string | null>(null)
  const plugins = useMemo(() => editorPlugins((error) => updateImageNote(uploadFailureText(error))), [])
  const input = { title, slug, content: markdown }
  const canSave = isSavable(input) && saveState.status !== 'saving'

  return (
    <FullScreenContext value={{ on: fullScreen, toggle: () => updateFullScreen((on) => !on) }}>
      <form className={fullScreen ? 'post-editor post-editor-full-screen' : 'post-editor'} aria-label="글 작성" onSubmit={(event) => event.preventDefault()}>
        <div className="post-editor-title-line">
          <div className="post-editor-field-group">
            <input
              className="post-editor-field"
              name="title"
              type="text"
              placeholder={`제목 (최대 ${POST_TITLE_MAX_LENGTH}자)`}
              aria-label="제목"
              value={title}
              onChange={(event) => updateTitle(event.target.value)}
            />
            <FieldHint note={saveNote(saveState)} />
          </div>
          <button className="post-editor-button" type="button" disabled={!canSave} onClick={() => onSave(input)}>저장</button>
          <button className="post-editor-button post-editor-publish" type="button" disabled={!onPublish} onClick={onPublish}>
            공개
          </button>
        </div>
        <div className="post-editor-field-group">
          <input
            className="post-editor-field"
            name="slug"
            type="text"
            placeholder={`Slug (최대 ${POST_SLUG_MAX_LENGTH}자)`}
            aria-label="Slug"
            value={slug}
            onChange={(event) => updateSlug(cleanSlug(event.target.value))}
          />
          <FieldHint note="영문 대소문자와 - 만 사용할 수 있습니다." />
        </div>
        <MDXEditor
          className="post-editor-body mdxeditor-full-height"
          contentEditableClassName="post-editor-content"
          markdown={markdown}
          onChange={(value) => {
            updateMarkdown(value)
            updateImageNote(null)
          }}
          placeholder="본문"
          plugins={plugins}
        />
        {imageNote !== null && <FieldHint note={imageNote} />}
        <input className="post-editor-field" name="hashtags" type="text" placeholder="#해시태그" aria-label="해시태그" />
      </form>
    </FullScreenContext>
  )
}
