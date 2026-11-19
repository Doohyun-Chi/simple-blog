/** Where an ad unit sits: above and below a post body, or in the info area. Each has its own AdSense ad unit. */
export type AdPosition = 'top' | 'bottom' | 'aside'

/** The publisher ID (`ca-pub-…`, the `data-ad-client`). It is public, not a secret; empty means ads are off. */
const CLIENT = (import.meta.env.VITE_ADSENSE_CLIENT ?? '').trim()

/** The ad unit IDs (`data-ad-slot`) from the AdSense console, one per position. A position without one shows no ad. */
const SLOTS: Record<AdPosition, string> = {
  top: (import.meta.env.VITE_ADSENSE_SLOT_TOP ?? '').trim(),
  bottom: (import.meta.env.VITE_ADSENSE_SLOT_BOTTOM ?? '').trim(),
  aside: (import.meta.env.VITE_ADSENSE_SLOT_ASIDE ?? '').trim(),
}

const SCRIPT_ID = 'adsense-script'

/** The ad unit for a position, or null when ads are off (development builds, no publisher ID, no unit ID). */
export function adUnitOf(position: AdPosition): { client: string; slot: string } | null {
  const slot = SLOTS[position]
  return import.meta.env.PROD && CLIENT !== '' && slot !== '' ? { client: CLIENT, slot } : null
}

/** Loads Google's ad script once, the first time an ad is about to show. Pages without an ad never load it. */
export function loadAdScript(client: string): void {
  if (document.getElementById(SCRIPT_ID) !== null) {
    return
  }
  const script = document.createElement('script')
  script.id = SCRIPT_ID
  script.async = true
  script.crossOrigin = 'anonymous'
  script.src = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${encodeURIComponent(client)}`
  document.head.appendChild(script)
}

/** Asks the script to fill an ad element that was just rendered. An element already filled is left alone. */
export function fillAd(element: HTMLElement): void {
  if (element.dataset.adsbygoogleStatus !== undefined) {
    return
  }
  const queue = ((window as Window & { adsbygoogle?: unknown[] }).adsbygoogle ??= [])
  queue.push({})
}
