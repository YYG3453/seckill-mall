import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 5173 为 Vite 默认端口，避开 Windows/Hyper-V 常保留的端口段（如 8111、8222 等会 EACCES）
    port: 5173,
    strictPort: false,
    // 只监听 IPv4，避免部分环境下对 :: 监听报 permission denied
    host: '127.0.0.1',
    proxy: {
      // 与 backend application.yml 中 server.port 一致（若你改成 8000，这里同步改）
      '/api': {
        target: 'http://127.0.0.1:8222',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://127.0.0.1:8222',
        changeOrigin: true
      }
    }
  }
})
