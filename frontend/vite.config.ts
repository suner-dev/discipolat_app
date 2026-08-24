import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: true,
    // Autorise les URL dynamiques des tunnels Cloudflare (trycloudflare.com)
    // pour que le dev server réponde aux hôtes publics.
    allowedHosts: ['.trycloudflare.com', '.cloudflare.com', '.ngrok-free.dev', '.ngrok.io', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          charts: ['recharts'],
          forms: ['react-hook-form', '@hookform/resolvers', 'zod'],
          query: ['@tanstack/react-query'],
          icons: ['lucide-react'],
          utils: ['axios', 'date-fns', 'clsx'],
        },
      },
    },
    chunkSizeWarningLimit: 300,
  },
});
