import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend_target = process.env.ERP_BACKEND_URL || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: backend_target,
        changeOrigin: true,
      },
      '/actuator': {
        target: backend_target,
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'es2022',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (id.includes('@ant-design/icons') || id.includes('/antd/') || id.includes('\\antd\\')) return 'vendor_antd';
          if (id.includes('react-router')) return 'vendor_router';
          if (id.includes('react-dom') || id.includes('/react/') || id.includes('\\react\\')) return 'vendor_react';
          return undefined;
        },
      },
    },
  },
});




