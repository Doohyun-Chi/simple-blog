import { TextInput } from 'design-system'
import type { ReactNode } from 'react'

interface PostEditorFieldProps {
  name: string
  label: string
  placeholder: string
  value: string
  onEdit: (value: string) => void
}

/** A single line text input of the editor. */
export function PostEditorField({ name, label, placeholder, value, onEdit }: PostEditorFieldProps) {
  return (
    <TextInput
      name={name}
      placeholder={placeholder}
      aria-label={label}
      value={value}
      onChange={(event) => onEdit(event.target.value)}
    />
  )
}

/** An input with the line of hints under it. */
export function HintedField({ note, ...field }: PostEditorFieldProps & { note: ReactNode }) {
  return (
    <div className="post-editor-field-group">
      <PostEditorField {...field} />
      {note}
    </div>
  )
}
