/** The GA4 measurement ID (`G-…`). It is public, not a secret; empty means analytics is off. Development and production use different ones. */
const MEASUREMENT_ID = (import.meta.env.VITE_GA_MEASUREMENT_ID ?? '').trim()

const SCRIPT_ID = 'ga-script'

/**
 * Starts Google Analytics 4 when a measurement ID is set. The page is reloaded on every navigation, so the page view that
 * `config` sends on load is the only one needed. A development build sends its events in debug mode (GA's DebugView).
 */
export function startAnalytics(): void {
  if (MEASUREMENT_ID === '' || document.getElementById(SCRIPT_ID) !== null) {
    return
  }
  const script = document.createElement('script')
  script.id = SCRIPT_ID
  script.async = true
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(MEASUREMENT_ID)}`
  document.head.appendChild(script)

  const layer = ((window as Window & { dataLayer?: unknown[] }).dataLayer ??= [])
  // gtag.js reads `arguments` objects from the data layer, so an arrow function pushing its array would not work.
  const gtag = function (..._args: unknown[]) {
    layer.push(arguments)
  }
  gtag('js', new Date())
  gtag('config', MEASUREMENT_ID, import.meta.env.DEV ? { debug_mode: true } : {})
}
