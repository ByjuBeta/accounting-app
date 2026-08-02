import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    // jsdom only implements localStorage for a real http(s) origin — without this,
    // window.localStorage is undefined, which breaks any zustand persist store under test.
    environmentOptions: {
      jsdom: {
        url: 'http://localhost:3000',
      },
    },
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
})
