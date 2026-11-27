import { Button } from 'design-system'
import { useContext } from 'react'
import { FullScreenContext } from '../model/FullScreen.ts'
import './FullScreenToggle.css'

/** The toolbar button at the right end: fills the whole browser area with the editor, and restores it when pressed again. */
export function FullScreenToggle() {
  const { on, toggle } = useContext(FullScreenContext)
  return (
    <Button size="sm" className="full-screen-toggle" aria-pressed={on} title={on ? '원래 크기로' : '전체 화면'} onClick={toggle}>
      <span aria-hidden="true">{on ? '⤡' : '⤢'}</span> {on ? '축소' : '전체 화면'}
    </Button>
  )
}
