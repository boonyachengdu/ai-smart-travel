<template>
  <n-config-provider>
    <div class="register-page">
      <n-card title="智能商旅后台管理系统 - 用户注册" :bordered="false" size="huge" class="register-card">
        <n-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-placement="left"
            label-width="80px"
            require-mark-placement="right-hanging"
        >
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="form.username" placeholder="请输入用户名（4-20 位字母数字）" clearable />
          </n-form-item>

          <n-form-item label="邮箱" path="email">
            <n-input v-model:value="form.email" placeholder="请输入邮箱地址" clearable />
          </n-form-item>

          <n-form-item label="手机号" path="phone">
            <n-input v-model:value="form.phone" placeholder="请输入手机号码" clearable />
          </n-form-item>

          <n-form-item label="密码" path="password">
            <n-input
                v-model:value="form.password"
                type="password"
                show-password-on="click"
                placeholder="请输入密码（6-20 位）"
                clearable
            />
          </n-form-item>

          <n-form-item label="确认密码" path="confirmPassword">
            <n-input
                v-model:value="form.confirmPassword"
                type="password"
                show-password-on="click"
                placeholder="请再次输入密码"
                clearable
            />
          </n-form-item>

          <n-form-item label="验证码" path="captcha">
            <n-space style="width: 100%">
              <n-input v-model:value="form.captcha" placeholder="请输入验证码" clearable style="flex: 1" />
              <n-button
                  type="primary"
                  :disabled="captchaDisabled"
                  :loading="captchaLoading"
                  @click="sendCaptcha"
              >
                {{ captchaText }}
              </n-button>
            </n-space>
          </n-form-item>

          <n-form-item>
            <n-space style="width: 100%">
              <n-button type="primary" @click="handleRegister" :loading="loading" size="large" block>
                注册
              </n-button>
              <n-button @click="goBackToLogin" size="large" block>
                返回登录
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-card>
    </div>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref, reactive, computed, inject } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const message = inject('naive-message') as any

const router = useRouter()

const loading = ref(false)
const captchaLoading = ref(false)
const captchaDisabled = ref(false)
const countdown = ref(60)
const formRef = ref<any>(null)

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const captchaText = computed(() => {
  if (captchaDisabled.value) {
    return `${countdown.value}s 后重新发送`
  }
  return '获取验证码'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
    {
      pattern: /^[a-zA-Z0-9]{4,20}$/,
      message: '用户名必须是 4-20 位字母或数字',
      trigger: ['input', 'blur']
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: ['input', 'blur'] },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: '请输入正确的邮箱格式',
      trigger: ['input', 'blur']
    }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: ['input', 'blur'] },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号码',
      trigger: ['input', 'blur']
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    {
      min: 6,
      max: 20,
      message: '密码必须是 6-20 位',
      trigger: ['input', 'blur']
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: ['input', 'blur'] },
    {
      validator: (rule: any, value: string) => {
        return value === form.password
      },
      message: '两次输入的密码不一致',
      trigger: ['input', 'blur']
    }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: ['input', 'blur'] }
  ]
}

const sendCaptcha = async () => {
  if (!form.email && !form.phone) {
    message?.warning('请先填写邮箱或手机号')
    return
  }

  captchaLoading.value = true
  try {
    await request.post('/auth/captcha', {
      email: form.email,
      phone: form.phone
    })
    message?.success('验证码已发送')

    captchaDisabled.value = true
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        captchaDisabled.value = false
        countdown.value = 60
      }
    }, 1000)
  } catch (err: any) {
    message?.error(`发送失败：${err.message || '请稍后重试'}`)
  } finally {
    captchaLoading.value = false
  }
}

const handleRegister = async () => {
  try {
    await formRef.value?.validate()

    if (form.password !== form.confirmPassword) {
      message?.warning('两次输入的密码不一致')
      return
    }

    loading.value = true

    await request.post('/auth/register', {
      username: form.username,
      email: form.email,
      phone: form.phone,
      password: form.password,
      captcha: form.captcha
    })

    message?.success('注册成功')
    router.push('/login')
  } catch (err: any) {
    if (err?.errors) {
      message?.error('请填写完整信息')
    } else {
      const errorMsg = err.message || err.response?.data?.message || '注册失败，请稍后重试'
      message?.error(errorMsg)
    }
  } finally {
    loading.value = false
  }
}

const goBackToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #37bb3d 0%, #764ba2 100%);
}

.register-card {
  width: 480px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
}

.n-card__header {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #2c3e50;
  padding: 32px 0 16px;
}
</style>
