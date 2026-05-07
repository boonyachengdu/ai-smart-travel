import axios from 'axios'

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 15000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器 - 添加 token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器 - 统一处理 401
request.interceptors.response.use(
    response => {
        return response.data
    },
    error => {
        const res = error.response?.data || {}

        if (error.response?.status === 401 || res.code === 401) {
            localStorage.removeItem('token')

            const currentPath = window.location.pathname + window.location.search
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
        }

        return Promise.reject(error)
    }
)

export default request
