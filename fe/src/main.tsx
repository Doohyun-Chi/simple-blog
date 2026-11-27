import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import 'design-system/styles.css'
import './index.css'
import App from './App.tsx'
import { startAnalytics } from './model/analytics.ts'

startAnalytics()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
