import {
  BlockTypeSelect,
  BoldItalicUnderlineToggles,
  type CodeBlockEditorDescriptor,
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
import { type FC, useMemo, useState } from 'react'
import {
  EMPTY_POST,
  POST_HASH_TAGS_MAX_LENGTH,
  POST_SLUG_MAX_LENGTH,
  POST_TITLE_MAX_LENGTH,
  type PostInput,
  PostType,
} from '../model/Post.ts'
import { FullScreenContext } from '../model/FullScreen.ts'
import { uploadFailureText } from '../model/Media.ts'
import { mediaFileUrl, uploadImage } from '../model/mediaApi.ts'
import { usePostDraft } from '../model/usePostDraft.ts'
import type { RemoveResult, SaveState } from '../model/useSavePost.ts'
import { FullScreenToggle } from './FullScreenToggle.tsx'
import { HintedField, PostEditorField } from './PostEditorField.tsx'
import { PostEditorActions } from './PostEditorActions.tsx'
import { FieldHint, SessionExpiredNote, SlugNote, TitleNote } from './PostEditorNotes.tsx'
import { ViewModeToggle } from './ViewModeToggle.tsx'
import { DrawioImageToolbar } from './DrawioImageToolbar.tsx'
import { InsertDrawio } from './InsertDrawio.tsx'
import { MediaPanel } from './MediaPanel.tsx'
import { InsertMermaid, MermaidCodeBlockEditor } from './MermaidCodeBlockEditor.tsx'
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
  mermaid: 'Mermaid',
}

/** A ```mermaid block is edited with its diagram drawn under the source; it outranks the plain code editor (priority 1). */
const MERMAID_CODE_BLOCK: CodeBlockEditorDescriptor = {
  priority: 10,
  match: (language) => language === 'mermaid',
  Editor: MermaidCodeBlockEditor,
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
    // imagePlugin types EditImageToolbar as taking no props, yet renders it with the image's props, as its own
    // editImageToolbarComponent$ cell (FC<EditImageToolbarProps>) says; hence the cast.
    imagePlugin({ imageUploadHandler: uploadedImageUrl, EditImageToolbar: DrawioImageToolbar as FC }),
    codeBlockPlugin({ defaultCodeBlockLanguage: '', codeBlockEditorDescriptors: [MERMAID_CODE_BLOCK] }),
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
            <InsertMermaid />
            <InsertDrawio />
          </ViewModeToggle>
          <FullScreenToggle />
        </>
      ),
    }),
  ]
}

const REMOVE_QUESTION = '이 글을 삭제할까요? 삭제한 글은 되돌릴 수 없습니다.'

interface PostEditorProps {
  /** Where the last save stands. */
  saveState: SaveState
  /** Saves the post: the first call creates a draft, later calls update it. Answers whether it was saved. */
  onSave: (input: PostInput) => Promise<boolean>
  /** Deletes the post saved so far. Answers how that ended. */
  onRemove: () => Promise<RemoveResult>
  /** The post to revise. Without it the editor starts empty. */
  initial?: PostInput
  /** Where the post is read, for the 보기 link. Defaults to a post. */
  contentType?: PostType
}

/**
 * The writing area from fe/DESIGN.md: the title with the save and publish buttons beside it, the slug, the MDX body
 * filling the rest, and the hashtags on the last line. The title and slug limits appear as placeholders only; the
 * backend refuses longer ones. The slug is limited to the characters the spec allows as it is typed. 공개 saves
 * the post as published and 삭제 deletes it, after asking. The hashtags are typed with or without # and sent as the
 * comma separated stored text. What the post holds and what the buttons do lives in usePostDraft.
 */
export function PostEditor({ saveState, onSave, onRemove, initial = EMPTY_POST, contentType = PostType.POST }: PostEditorProps) {
  const draft = usePostDraft({ initial, saveState, onSave, onRemove })
  const [fullScreen, updateFullScreen] = useState(false)
  const [imageNote, updateImageNote] = useState<string | null>(null)
  const plugins = useMemo(() => editorPlugins((error) => updateImageNote(uploadFailureText(error))), [])
  return (
    <FullScreenContext value={{ on: fullScreen, toggle: () => updateFullScreen((on) => !on) }}>
      <form className={fullScreen ? 'post-editor post-editor-full-screen' : 'post-editor'} aria-label="글 작성" onSubmit={(event) => event.preventDefault()}>
        <div className="post-editor-title-line">
          <HintedField
            name="title"
            label="제목"
            placeholder={`제목 (최대 ${POST_TITLE_MAX_LENGTH}자)`}
            value={draft.title}
            onEdit={draft.editTitle}
            note={<TitleNote saveState={saveState} unsaved={draft.unsaved} published={draft.published} contentType={contentType}
              removeFailure={draft.removeFailure} />}
          />
          <PostEditorActions
            canSave={draft.canSave}
            canRemove={draft.canRemove}
            published={draft.published}
            onSave={() => void draft.save()}
            onPublish={() => void draft.publish()}
            onRemove={() => void draft.remove(() => window.confirm(REMOVE_QUESTION))}
          />
        </div>
        <HintedField
          name="slug"
          label="Slug"
          placeholder={`Slug (최대 ${POST_SLUG_MAX_LENGTH}자)`}
          value={draft.slug}
          onEdit={draft.editSlug}
          note={<SlugNote slug={draft.slug} savedSlug={draft.savedSlug} />}
        />
        <MDXEditor
          className="post-editor-body mdxeditor-full-height"
          contentEditableClassName="post-editor-content"
          markdown={draft.content}
          onChange={(value) => {
            draft.editContent(value)
            updateImageNote(null)
          }}
          placeholder="본문"
          plugins={plugins}
        />
        {imageNote !== null && <FieldHint note={imageNote} />}
        {draft.sessionExpired && <SessionExpiredNote />}
        <PostEditorField
          name="hashtags"
          label="해시태그"
          placeholder={`#해시태그, 쉼표로 구분 (최대 ${POST_HASH_TAGS_MAX_LENGTH}자)`}
          value={draft.typedTags}
          onEdit={draft.editTags}
        />
      </form>
    </FullScreenContext>
  )
}
