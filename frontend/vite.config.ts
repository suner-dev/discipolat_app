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
    // Autorise les URL dynamiques des tunnels (ngrok, Cloudflare trycloudflare)
    // pour que le dev server réponde aux hôtes publics.
    allowedHosts: ['.trycloudflare.com', '.cloudflare.com', '.ngrok-free.dev', '.ngrok-free.app', '.ngrok.io', 'localhost'],
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
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('/recharts/')) return 'charts';
          if (id.includes('/react-hook-form/') || id.includes('/@hookform/') || id.includes('/zod/')) return 'forms';
          if (id.includes('/@tanstack/react-query/')) return 'query';
          if (id.includes('/lucide-react/')) return 'icons';
          if (id.includes('/axios/') || id.includes('/date-fns/') || id.includes('/clsx/')) return 'utils';
          return 'vendor';
        },
      },
    },
    chunkSizeWarningLimit: 300,
  },
});
