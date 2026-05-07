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

        <!-- 专业菜单 - 数据看板独立 + 模块分组 -->
        <n-menu
            v-model:value="activeKey"
            :collapsed="collapsed"
            :options="menuOptions"
            @update:value="handleUpdateValue"
            style="background: transparent; color: #e5e7eb !important;"
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
import { ref, computed, h, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
import { Icon } from '@iconify/vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

// 菜单选项 - 数据看板独立 + 模块分组
const menuOptions = [
  // ==================== 数据看板（独立模块） ====================
  {
    label: '数据看板',
    key: 'dashboard',
    icon: () => h(Icon, { icon: 'mdi:dashboard', width: '20', height: '20' })
  },
  // ==================== 系统管理 ====================
  {
    label: '系统管理',
    key: 'system',
    icon: () => h(Icon, { icon: 'mdi:cog-outline', width: '20', height: '20' }),
    children: [
      { label: '企业管理', key: 'company', icon: () => h(Icon, { icon: 'mdi:domain', width: '20', height: '20' }) },
      { label: '部门管理', key: 'department', icon: () => h(Icon, { icon: 'mdi:folder-account-outline', width: '20', height: '20' }) },
      { label: '员工管理', key: 'employee', icon: () => h(Icon, { icon: 'mdi:account-tie-outline', width: '20', height: '20' }) },
      { label: '用户管理', key: 'user', icon: () => h(Icon, { icon: 'mdi:account-group-outline', width: '20', height: '20' }) }
    ]
  },
  // ==================== 出行管理 ====================
  {
    label: '出行管理',
    key: 'travel',
    icon: () => h(Icon, { icon: 'mdi:airplane-takeoff', width: '20', height: '20' }),
    children: [
      { label: '订单管理', key: 'order', icon: () => h(Icon, { icon: 'mdi:cart-outline', width: '20', height: '20' }) },
      { label: '差标管理', key: 'standard', icon: () => h(Icon, { icon: 'mdi:shield-check-outline', width: '20', height: '20' }) },
      { label: '流程审批', key: 'approval', icon: () => h(Icon, { icon: 'mdi:check-decagram-outline', width: '20', height: '20' }) }
    ]
  },
  // ==================== 智能管理 ====================
  {
    label: '智能管理',
    key: 'smart',
    icon: () => h(Icon, { icon: 'mdi:robot-outline', width: '20', height: '20' }),
    children: [
      { label: '文档记录', key: 'rag', icon: () => h(Icon, { icon: 'mdi:book-open-variant-outline', width: '20', height: '20' }) },
      { label: '差旅政策', key: 'policy', icon: () => h(Icon, { icon: 'mdi:file-document-outline', width: '20', height: '20' }) },
      { label: '提示词管理', key: 'prompt', icon: () => h(Icon, { icon: 'mdi:comment-text-multiple-outline', width: '20', height: '20' }) },
      { label: '会话管理', key: 'session', icon: () => h(Icon, { icon: 'mdi:chat-processing-outline', width: '20', height: '20' }) }
    ]
  },
  // ==================== Agent 管理 ====================
  {
    label: 'Agent 管理',
    key: 'agent',
    icon: () => h(Icon, { icon: 'mdi:brain', width: '20', height: '20' }),
    children: [
      { label: '记忆管理', key: 'agent-memory', icon: () => h(Icon, { icon: 'mdi:memory', width: '20', height: '20' }) },
      { label: '反馈分析', key: 'agent-feedback', icon: () => h(Icon, { icon: 'mdi:thumbs-up-down-outline', width: '20', height: '20' }) }
    ]
  }
]

// 所有菜单项的 key 集合（用于路由匹配）
const allMenuKeys = [
  'dashboard',
  'company', 'department', 'employee', 'user',
  'order', 'standard', 'approval',
  'rag', 'policy', 'prompt', 'session',
  'agent-memory', 'agent-feedback'
]

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const activeKey = ref('dashboard')

// 监听路由变化，设置当前激活的菜单项
watch(() => route.path, (newPath) => {
  const path = newPath.substring(1)
  if (path && allMenuKeys.includes(path)) {
    activeKey.value = path
  }
}, { immediate: true })

// 处理菜单点击
const handleUpdateValue = (key: string) => {
  router.push(`/${key}`)
}

// 退出登录
const logout = () => {
  request.post('/auth/logout')
  localStorage.removeItem('token')
  router.push('/login')
}

const userName = computed(() => userStore.userInfo?.username || '管理员')

// 主题覆盖
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
    itemColorActive: '#10b981',
    itemTextColorActive: '#ffffff',
    itemTextColor: '#d1d5db',
    itemTextColorHover: '#ffffff',
    itemColorHover: 'rgba(255,255,255,0.12)',
    itemTextColorChildActive: '#ffffff'
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

.n-menu-item {
  transition: all 0.2s;
}

.n-menu-item:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #ffffff !important;
}

/* 强制显示 Iconify 图标 */
.n-menu-item__icon {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 20px !important;
  height: 20px !important;
  margin-right: 8px !important;
  color: #d1d5db !important;
  transition: color 0.3s;
  font-size: 20px !important;
  line-height: 1 !important;
}

.n-menu-item--selected .n-menu-item__icon,
.n-menu-item:hover .n-menu-item__icon {
  color: #ffffff !important;
}

/* 折叠时图标居中 */
.n-menu--collapsed .n-menu-item__icon {
  margin: 0 auto !important;
}

/* 确保 SVG 显示 */
.n-menu-item__icon svg {
  width: 100% !important;
  height: 100% !important;
  fill: currentColor !important;
}
</style>