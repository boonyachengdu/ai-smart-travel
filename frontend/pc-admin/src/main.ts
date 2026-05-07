// src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import NaiveUI, { createDiscreteApi } from 'naive-ui'
import App from './App.vue'
import router from './router/index'

// 创建 Naive UI 的 discrete API（包含 message、dialog、notification 等）
const { message, notification, dialog, loadingBar } = createDiscreteApi([
    'message',
    'notification',
    'dialog',
    'loadingBar'
])

const app = createApp(App)
const pinia = createPinia()

pinia.use(piniaPluginPersistedstate)
app.use(pinia)
app.use(router)
app.use(NaiveUI)

// 全局注入 message 等 API（这样任何组件都能直接 useMessage()）
app.provide('naive-message', message)
app.provide('naive-notification', notification)
app.provide('naive-dialog', dialog)
app.provide('naive-loading-bar', loadingBar)

app.mount('#app')
