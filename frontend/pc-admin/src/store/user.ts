// src/store/user.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
    const token = ref<string | null>(localStorage.getItem('token') || null)
    const userInfo = ref<any>(null)

    // 登录
    const login = async (credentials: { username: string; password: string }) => {
        try {
            const res = await request.post('/auth/login', credentials)
            if (res.code === 200) {
                token.value = res.data.token
                userInfo.value = res.data.user || {}
                localStorage.setItem('token', res.data.token)
                localStorage.setItem('userInfo', JSON.stringify(res.data.user || {}))
                return res
            }
            throw new Error(res.message || '登录失败')
        } catch (err: any) {
            throw err
        }
    }

    // 退出
    const logout = () => {
        token.value = null
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return { token, userInfo, login, logout }
}, {
    persist: true // 持久化到 localStorage
})