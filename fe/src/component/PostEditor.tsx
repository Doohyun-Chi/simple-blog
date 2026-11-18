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
  EMPTY_POST,
  isSavable,
  isUnsaved,
  POST_SLUG_MAX_LENGTH,
  POST_TITLE_MAX_LENGTH,
  type PostInput,
} from '../model/Post.ts'
import { FullScreenContext } from '../model/FullScreen.ts'
import { uploadFailureText } from '../model/Media.ts'
import { mediaFileUrl, uploadImage } from '../model/mediaApi.ts'
import { googleLoginUrl } from '../model/identityApi.ts'
import { contentPath } from '../model/route.ts'
import { useLeaveGuard } from '../model/useLeaveGuard.ts'
import type { SaveFailure, SaveState } from '../model/useSavePost.ts'
import { useSessionKeepAlive } from '../model/useSessionKeepAlive.ts'
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
  expired: '로그인이 만료되어 저장하지 못했습니다.',
  forbidden: '저장할 권한이 없습니다.',
  missing: '저장된 글을 찾을 수 없습니다. 다시 저장하면 새 글로 저장합니다.',
  failed: '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
}

interface PostEditorProps {
  /** Where the last save stands. */
  saveState: SaveState
  /** Saves the post: the first call creates a draft, later calls update it. Answers whether it was saved. */
  onSave: (input: PostInput) => Promise<boolean>
  /** The post to revise. Without it the editor starts empty. */
  initial?: PostInput
}

/** The line under a field. */
function FieldHint({ note }: { note: ReactNode }) {
  return (
    <div className="post-editor-hint">
      <span className="post-editor-note" role="status">{note}</span>
    </div>
  )
}

/** The line under the title: the save in progress or failed, else a warning while changes are unsaved, else the last save. */
function noteOf(state: SaveState, unsaved: boolean, published: boolean): ReactNode {
  const busyOrFailed = state.status === 'saving' || state.status === 'failed'
  return unsaved && !busyOrFailed ? '저장하지 않은 변경이 있습니다.' : saveNote(state, published)
}

/** Tells the writer the session ended and how to sign in again without leaving the editor, so nothing written is lost. */
function SessionExpiredNote() {
  return (
    <FieldHint
      note={
        <>
          로그인이 만료되었습니다.
          {' '}
          <a href={googleLoginUrl()} target="_blank" rel="noreferrer">새 탭에서 다시 로그인</a>
          한 뒤 저장해 주세요. 작성 중인 내용은 이 탭에 그대로 남아 있습니다.
        </>
      }
    />
  )
}

function saveNote(state: SaveState, published: boolean): ReactNode {
  switch (state.status) {
    case 'idle':
      return null
    case 'saving':
      return '저장 중…'
    case 'saved':
      return <>{published ? '저장했습니다.' : '초안으로 저장했습니다.'} <a href={contentPath('post', state.slug)}>보기</a></>
    case 'failed':
      return SAVE_FAILURE_TEXT[state.failure]
  }
}

/**
 * The writing area from fe/DESIGN.md: the title with the save and publish buttons beside it, the slug, the MDX body
 * filling the rest, and the hashtags on the last line. The title and slug limits appear as placeholders only; the
 * backend refuses longer ones. The slug is limited to the characters the spec allows as it is typed. 공개 saves
 * the post as published. Hashtags have no field in the spec, so they are not sent.
 */
export function PostEditor({ saveState, onSave, initial = EMPTY_POST }: PostEditorProps) {
  const [title, updateTitle] = useState(initial.title)
  const [slug, updateSlug] = useState(initial.slug)
  const [markdown, updateMarkdown] = useState(initial.content)
  const [fullScreen, updateFullScreen] = useState(false)
  const [saved, updateSaved] = useState<PostInput>(initial)
  const [published, updatePublished] = useState(initial.status === 'publish')
  const keepAlive = useSessionKeepAlive()
  const [imageNote, updateImageNote] = useState<string | null>(null)
  const plugins = useMemo(() => editorPlugins((error) => updateImageNote(uploadFailureText(error))), [])
  const input = { title, slug, content: markdown }
  const canSave = isSavable(input) && saveState.status !== 'saving'
  const unsaved = isUnsaved(input, saved)
  const sessionExpired = keepAlive.expired || (saveState.status === 'failed' && saveState.failure === 'expired')
  useLeaveGuard(unsaved)

  async function saveNow() {
    if (await onSave(input)) {
      updateSaved(input)
    }
  }

  /** Saves the post as published, creating it first when it was never saved. */
  async function publishNow() {
    if (await onSave({ ...input, status: 'publish' })) {
      updateSaved(input)
      updatePublished(true)
    }
  }

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
              onChange={(event) => {
              updateTitle(event.target.value)
              keepAlive.touch()
            }}
            />
            <FieldHint note={noteOf(saveState, unsaved, published)} />
          </div>
          <button className="post-editor-button" type="button" disabled={!canSave} onClick={() => void saveNow()}>저장</button>
          <button
            className="post-editor-button post-editor-publish"
            type="button"
            disabled={!canSave || published}
            onClick={() => void publishNow()}
          >
            {published ? '공개됨' : '공개'}
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
            onChange={(event) => {
            updateSlug(cleanSlug(event.target.value))
            keepAlive.touch()
          }}
          />
          <FieldHint note="영문 대소문자와 - 만 사용할 수 있습니다." />
        </div>
        <MDXEditor
          className="post-editor-body mdxeditor-full-height"
          contentEditableClassName="post-editor-content"
          markdown={markdown}
          onChange={(value) => {
            updateMarkdown(value)
          keepAlive.touch()
            updateImageNote(null)
          }}
          placeholder="본문"
          plugins={plugins}
        />
        {imageNote !== null && <FieldHint note={imageNote} />}
      {sessionExpired && <SessionExpiredNote />}
        <input className="post-editor-field" name="hashtags" type="text" placeholder="#해시태그" aria-label="해시태그" />
      </form>
    </FullScreenContext>
  )
}
