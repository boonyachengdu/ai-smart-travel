import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { fileURLToPath, URL } from 'node:url'
import commonjs from 'vite-plugin-commonjs'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [VantResolver()],
    }),
    commonjs(), // 解决 markdown-it 的 CJS 问题
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'markdown-it': 'markdown-it/dist/markdown-it.min.js',
    },
  },
  server: {
    port: 5174,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => {
          console.log('Vite proxy rewrite:', path); // 加这行日志
          return path.replace(/^\/api/, '')
        }
      },
    },
    allowedHosts: [
      'undaggled-roxanne-unpoetic.ngrok-free.dev'  // 添加你的 ngrok 域名
    ]
  },
})
