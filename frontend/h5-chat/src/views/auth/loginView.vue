<template>
  <div class="login-page">
    <!-- 背景渐变 + 模糊效果 -->
    <div class="background"></div>

    <van-nav-bar title="智能商旅助手" left-arrow @click-left="goBack" />

    <div class="login-container">
      <div class="login-card">
        <div class="header">
          <h1>欢迎登录</h1>
          <p>智能差旅 · 高效出行</p>
        </div>

        <van-cell-group inset>
          <van-field
              v-model="username"
              label="用户名"
              placeholder="请输入用户名 / 手机号"
              required
              clearable
          />
          <van-field
              v-model="password"
              type="password"
              label="密码"
              placeholder="请输入密码"
              required
              clearable
              show-password
          />
        </van-cell-group>

        <div class="actions">
          <van-button
              type="primary"
              size="large"
              block
              round
              :loading="loading"
              @click="login"
          >
            登录
          </van-button>
        </div>

        <div class="footer">
          <p>© 2026 智能商旅系统 - 内部使用平台</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

const username = ref('')
const password = ref('')
const loading = ref(false)
const router = useRouter()

const login = async () => {
  if (!username.value.trim() || !password.value.trim()) {
    showToast('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/auth/login', {
      username: username.value,
      password: password.value,
    })

    // 打印完整返回，便于调试（可上线后删除）
    console.log('登录返回完整数据：', res)

    // 正确取 token（根据你的返回结构）
    const token = res.data?.token

    if (!token) {
      showToast('登录成功但未返回 token，请联系管理员')
      return
    }

    localStorage.setItem('token', token)
    console.log('token 已存入：', token.substring(0, 20) + '...') // 安全打印前20位

    // 可选：同时存用户信息（如果需要）
    // localStorage.setItem('userInfo', JSON.stringify(res.data.user))

    showToast('登录成功')
    router.push('/')
  } catch (err) {
    console.error('登录失败：', err)
    showToast('登录失败，请检查账号密码或网络')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  position: relative;
  overflow: hidden;
}

.background {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #37bb3d 0%, #764ba2 100%);
  filter: blur(8px);
  z-index: -1;
}

.login-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 32px 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.actions {
  margin-top: 32px;
}

.van-button--primary {
  background: #10b981 !important;
  border: none !important;
  height: 48px !important;
  font-size: 16px !important;
}

.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: #999;
}
</style>