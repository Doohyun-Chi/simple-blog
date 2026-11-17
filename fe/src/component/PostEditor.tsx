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
import { useState } from 'react'
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

interface PostEditorProps {
  /** Saves the post as a draft. While it is missing, the 저장 button stays disabled. */
  onSave?: () => void
  /** Publishes the post. While it is missing, the 공개 button stays disabled. */
  onPublish?: () => void
}

/**
 * The writing area from fe/DESIGN.md: the title with the save and publish buttons beside it, the slug, the MDX body
 * filling the rest, and the hashtags on the last line. The spec has no endpoint for creating a post yet, so nothing
 * is wired to the buttons and what is typed lives only in this form.
 */
export function PostEditor({ onSave, onPublish }: PostEditorProps) {
  const [markdown, updateMarkdown] = useState('')

  return (
    <form className="post-editor" aria-label="글 작성" onSubmit={(event) => event.preventDefault()}>
      <div className="post-editor-title-line">
        <input className="post-editor-field" name="title" type="text" placeholder="제목" aria-label="제목" />
        <button className="post-editor-button" type="button" disabled={!onSave} onClick={onSave}>저장</button>
        <button className="post-editor-button post-editor-publish" type="button" disabled={!onPublish} onClick={onPublish}>
          공개
        </button>
      </div>
      <input className="post-editor-field" name="slug" type="text" placeholder="Slug" aria-label="Slug" />
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
