/// <reference types="vitest" />
/// <reference types="vite/client" />

import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],

  build: {
    outDir: '../backend/src/main/resources/public'
  },

  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },

  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
  },
})
