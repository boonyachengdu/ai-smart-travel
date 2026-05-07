import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': '/src'
    }
  },
  server: {
    port: 5173,
    open: true,
    proxy: {
      // 代理到后端 gateway（根据你的实际 gateway 端口调整）
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => {
          console.log('Vite proxy rewrite:', path); // 加这行日志
          return path.replace(/^\/api/, '')
        }
      }
    },
    allowedHosts: [
      'undaggled-roxanne-unpoetic.ngrok-free.dev'  // 添加你的 ngrok 域名
    ]
  }
})