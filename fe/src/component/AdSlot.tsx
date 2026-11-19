import { useEffect, useRef } from 'react'
import { type AdPosition, adUnitOf, fillAd, loadAdScript } from '../model/adsense.ts'
import './AdSlot.css'

interface AdSlotProps {
  position: AdPosition
}

/** One AdSense display ad. It renders nothing, and loads nothing, when ads are off for the position. */
export function AdSlot({ position }: AdSlotProps) {
  const unit = adUnitOf(position)
  const element = useRef<HTMLModElement>(null)
  const requested = useRef<HTMLModElement | null>(null)
  const client = unit?.client
  useEffect(() => {
    // The script marks an element as filled only after it loads, so remember the request here to push once per element.
    if (client !== undefined && element.current !== null && requested.current !== element.current) {
      requested.current = element.current
      loadAdScript(client)
      fillAd(element.current)
    }
  }, [client])
  if (unit === null) {
    return null
  }
  return (
    <div className={`ad-slot ad-slot-${position}`} aria-label="광고">
      <ins
        ref={element}
        className="adsbygoogle"
        style={{ display: 'block' }}
        data-ad-client={unit.client}
        data-ad-slot={unit.slot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </div>
  )
}
