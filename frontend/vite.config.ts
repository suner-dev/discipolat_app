import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

/**
 * Backend que le serveur de développement relaie (/api et /ws-church).
 * Surchargeable par `API_PROXY_TARGET` quand le port 8080 est occupé.
 */
const apiProxyTarget = process.env.API_PROXY_TARGET || 'http://localhost:8080';

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
      // Cible du backend paramétrable, défaut inchangé (localhost:8080).
      // Nécessaire quand le port 8080 est déjà occupé (autre conteneur, autre
      // instance) : `API_PROXY_TARGET=http://localhost:8090 npm run dev`.
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      // §G5.8 — bus temps réel STOMP/SockJS (firehose outbox web ↔ mobile)
      '/ws-church': {
        target: apiProxyTarget,
        changeOrigin: true,
        ws: true,
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
