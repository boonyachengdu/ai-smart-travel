<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-layout has-sider position="absolute" style="height: 100vh;">
      <!-- 左侧菜单 - 深灰专业风格 -->
      <n-layout-sider
          bordered
          collapse-mode="width"
          :collapsed-width="64"
          :width="240"
          :collapsed="collapsed"
          show-trigger
          @collapse="collapsed = $event"
          style="background: linear-gradient(180deg, #1f2937 0%, #111827 100%); box-shadow: 3px 0 12px rgba(0,0,0,0.25);"
      >
        <!-- Logo -->
        <div class="logo-wrapper" :class="{ collapsed }">
          <n-h3 v-if="!collapsed" prefix="bar" style="margin: 0; padding: 24px 16px; color: #ffffff; font-weight: 700; letter-spacing: 1px;">
            智能商旅
          </n-h3>
          <n-h3 v-else style="margin: 24px 0; text-align: center; color: #ffffff; font-weight: 700;">
            ST
          </n-h3>
        </div>

        <!-- 菜单 - 强制高对比度 -->
        <n-menu
            v-model:value="activeKey"
            :collapsed="collapsed"
            :options="menuOptions"
            @update:value="handleUpdateValue"
            style="background: transparent; color: #ffffff !important;"
        />
      </n-layout-sider>

      <!-- 右侧主区域 -->
      <n-layout>
        <n-layout-header
            bordered
            style="height: 64px; padding: 0 32px; display: flex; align-items: center; justify-content: space-between; background: #ffffff; box-shadow: 0 4px 12px rgba(0,0,0,0.08); z-index: 10;"
        >
          <n-h2 style="margin: 0; color: #1f2937; font-weight: 600; letter-spacing: 0.5px;">
            智能商旅后台管理系统
          </n-h2>

          <n-space align="center">
            <n-avatar round size="medium" src="https://07akioni.oss-cn-beijing.aliyuncs.com/akio.jpg" />
            <span style="font-weight: 500; color: #1f2937; margin-left: 8px;">{{ userName || '管理员' }}</span>
            <n-button type="primary" size="small" @click="logout">退出登录</n-button>
          </n-space>
        </n-layout-header>

        <n-layout-content style="padding: 32px; overflow: auto; background: #f8fafc;">
          <n-card
              :bordered="false"
              style="border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.08); min-height: calc(100vh - 128px); background: #ffffff;"
          >
            <router-view />
          </n-card>
        </n-layout-content>
      </n-layout>
    </n-layout>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  NConfigProvider,
  NLayout,
  NLayoutSider,
  NLayoutHeader,
  NLayoutContent,
  NMenu,
  NButton,
  NAvatar,
  NH3,
  NH2,
  NSpace
} from 'naive-ui'
import { useUserStore } from '@/store/user'

import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const activeKey = ref('user')

const menuOptions = [
  { label: '企业管理', key: 'company' },
  { label: '部门管理', key: 'department' },
  { label: '员工管理', key: 'employee' },
  { label: '用户管理', key: 'user' },
  { label: '订单管理', key: 'order' },
  { label: '差标管理', key: 'standard' },
  { label: '流程审批', key: 'approval' },
  { label: 'RAG 知识库', key: 'rag' },
  { label: '提示词管理', key: 'prompt' },
  { label: '会话管理', key: 'session' }
]

const handleUpdateValue = (key: string) => {
  router.push(`/${key}`)
}

const logout = () => {
  request.post('/auth/logout')
  localStorage.removeItem('token')
  router.push('/login')
}

const userName = computed(() => userStore.userInfo?.username || '管理员')

// 主题覆盖（加强菜单文字可见度）
const themeOverrides = {
  common: {
    primaryColor: '#10b981',
    primaryColorHover: '#34d399',
    primaryColorPressed: '#059669',
    borderRadius: '12px'
  },
  Card: {
    borderRadius: '16px'
  },
  Menu: {
    itemColorActive: '#10b981',              // 选中背景
    itemTextColorActive: '#ffffff',           // 选中文字（最亮白）
    itemTextColor: '#f3f4f6',                 // 普通文字（浅灰白，高对比）
    itemTextColorHover: '#ffffff',            // hover 文字
    itemColorHover: 'rgba(255,255,255,0.12)', // hover 背景（更明显）
    itemTextColorChildActive: '#ffffff'       // 子菜单选中文字
  }
}
</script>

<style scoped>
.logo-wrapper {
  padding: 24px 16px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.3s;
}

.logo-wrapper.collapsed {
  padding: 24px 0;
}

/* 菜单项 hover 更明显 */
.n-menu-item {
  transition: all 0.2s;
}

.n-menu-item:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #ffffff !important;
}
</style>