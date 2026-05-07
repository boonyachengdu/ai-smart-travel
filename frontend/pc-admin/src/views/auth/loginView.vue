<template>
  <n-config-provider>
    <div class="login-page">
      <n-card title="智能商旅后台管理系统" :bordered="false" size="huge" class="login-card">
        <n-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-placement="left"
            label-width="80px"
            require-mark-placement="right-hanging"
            @submit.prevent="handleLogin"
        >
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="form.username" placeholder="请输入用户名" clearable />
          </n-form-item>

          <n-form-item label="密码" path="password">
            <n-input
                v-model:value="form.password"
                type="password"
                show-password-on="click"
                placeholder="请输入密码"
                clearable
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" attr-type="submit" :loading="loading" size="large" block>
              登录
            </n-button>
          </n-form-item>

          <n-divider style="margin: 16px 0">或</n-divider>

          <n-space justify="center">
            <n-button text type="primary" @click="goToRegister">
              没有账号？立即注册
            </n-button>
          </n-space>
        </n-form>
      </n-card>
    </div>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref, reactive, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const message = inject('naive-message') as any

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref<any>(null)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] }
  ]
}

const handleLogin = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) {
      message?.warning('请填写完整信息')
      return
    }

    loading.value = true
    try {
      await userStore.login({
        username: form.username,
        password: form.password
      })
      message?.success('登录成功')
      router.push('/')
    } catch (err: any) {
      message?.error(err.message || '登录失败，请检查用户名或密码')
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #37bb3d 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
}

.n-card__header {
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  padding: 32px 0 16px;
}
</style>
