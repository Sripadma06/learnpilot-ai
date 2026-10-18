import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The production build is written straight into Spring Boot's static folder, so the
// finished app is served by Spring on http://localhost:8080 (no separate frontend server).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: false,
    // Only used for `npm run dev` (hot reload): forwards API calls to the Spring Boot app.
    proxy: { '/api': 'http://localhost:8080' }
  },
  build: { outDir: '../resources/static', emptyOutDir: true }
})
