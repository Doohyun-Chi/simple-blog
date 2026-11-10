import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
// The backend endpoint is configured per mode in .env.development / .env.production (VITE_API_BASE_URL).
export default defineConfig({
  plugins: [react()],
})
