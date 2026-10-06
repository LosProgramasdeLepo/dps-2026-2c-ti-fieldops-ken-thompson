import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const { FIELDOPS_API = 'http://localhost:8080' } = loadEnv(mode, '.', 'FIELDOPS_');
  const backend = { '/v1': FIELDOPS_API };
  return {
    plugins: [react()],
    server: { proxy: backend },
    preview: { proxy: backend },
  };
});
