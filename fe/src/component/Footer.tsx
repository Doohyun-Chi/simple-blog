import './Footer.css'

const COPYRIGHT_YEAR = new Date().getFullYear()

export function Footer() {
  return (
    <footer className="footer">
      <p className="footer-text">© {COPYRIGHT_YEAR} Dreaming for the Future. All rights reserved.</p>
    </footer>
  )
}
