import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Forward FE API calls to the Spring Boot backend during `npm run dev`.
    proxy: {
      '/public/api': 'http://localhost:8080',
    },
  },
})
