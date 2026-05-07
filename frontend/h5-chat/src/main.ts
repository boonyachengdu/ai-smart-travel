import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import 'vant/lib/index.css'
import router from './router'

console.log('main.ts 执行了，准备挂载 App')

const app = createApp(App)
app.use(createPinia())
app.use(router)

try {
    app.mount('#app')
    console.log('App 挂载成功')
} catch (err) {
    console.error('挂载失败', err)
}
