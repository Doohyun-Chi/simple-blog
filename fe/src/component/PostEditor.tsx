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
  headingsPlugin,
  linkDialogPlugin,
  linkPlugin,
  listsPlugin,
  markdownShortcutPlugin,
  quotePlugin,
  thematicBreakPlugin,
  toolbarPlugin,
} from '@mdxeditor/editor'
import '@mdxeditor/editor/style.css'
import { type ReactNode, useState } from 'react'
import {
  characterCount,
  fitSlug,
  fitTitle,
  isSavable,
  POST_SLUG_MAX_LENGTH,
  POST_TITLE_MAX_LENGTH,
  type PostInput,
} from '../model/Post.ts'
import { contentPath } from '../model/route.ts'
import type { SaveFailure, SaveState } from '../model/useSavePost.ts'
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

/** Plugins for the body editor: Markdown formatting, links and code blocks, and a toolbar for them. */
const EDITOR_PLUGINS = [
  headingsPlugin(),
  listsPlugin(),
  quotePlugin(),
  thematicBreakPlugin(),
  linkPlugin(),
  linkDialogPlugin(),
  codeBlockPlugin({ defaultCodeBlockLanguage: '' }),
  codeMirrorPlugin({ codeBlockLanguages: CODE_LANGUAGES }),
  markdownShortcutPlugin(),
  toolbarPlugin({
    toolbarContents: () => (
      <>
        <UndoRedo />
        <BlockTypeSelect />
        <BoldItalicUnderlineToggles />
        <CodeToggle />
        <ListsToggle />
        <CreateLink />
        <InsertCodeBlock />
      </>
    ),
  }),
]

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

/** The line under a field: a note on the left and a length count on the right. */
function FieldHint({ note, count, max }: { note: ReactNode; count: number; max: number }) {
  return (
    <div className="post-editor-hint">
      <span className="post-editor-note" role="status">{note}</span>
      <span className="post-editor-count">{count}/{max}</span>
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
 * filling the rest, and the hashtags on the last line. Title and slug are held to the backend's limits as they are
 * typed. 공개 stays disabled: the spec has no publish operation, and hashtags have no field in it, so they are not sent.
 */
export function PostEditor({ saveState, onSave, onPublish }: PostEditorProps) {
  const [title, updateTitle] = useState('')
  const [slug, updateSlug] = useState('')
  const [markdown, updateMarkdown] = useState('')
  const input = { title, slug, content: markdown }
  const canSave = isSavable(input) && saveState.status !== 'saving'

  return (
    <form className="post-editor" aria-label="글 작성" onSubmit={(event) => event.preventDefault()}>
      <div className="post-editor-title-line">
        <div className="post-editor-field-group">
          <input
            className="post-editor-field"
            name="title"
            type="text"
            placeholder="제목"
            aria-label="제목"
            value={title}
            onChange={(event) => updateTitle(fitTitle(event.target.value))}
          />
          <FieldHint note={saveNote(saveState)} count={characterCount(title)} max={POST_TITLE_MAX_LENGTH} />
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
          placeholder="Slug"
          aria-label="Slug"
          value={slug}
          onChange={(event) => updateSlug(fitSlug(event.target.value))}
        />
        <FieldHint note="영문 대소문자와 - 만 사용할 수 있습니다." count={slug.length} max={POST_SLUG_MAX_LENGTH} />
      </div>
      <MDXEditor
        className="post-editor-body"
        contentEditableClassName="post-editor-content"
        markdown={markdown}
        onChange={updateMarkdown}
        placeholder="본문"
        plugins={EDITOR_PLUGINS}
      />
      <input className="post-editor-field" name="hashtags" type="text" placeholder="#해시태그" aria-label="해시태그" />
    </form>
  )
}
