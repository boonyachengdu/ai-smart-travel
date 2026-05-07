<template>
  <div class="my-center">
    <van-nav-bar title="我的" />

    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-avatar">
        <van-icon name="manager-o" size="40" color="#fff" />
      </div>
      <div class="user-info">
        <div class="user-name">{{ userInfo?.employeeName || '用户' }}</div>
        <div class="user-desc">{{ userInfo?.departmentName || '智能差旅助手' }}</div>
      </div>
    </div>

    <!-- 退出登录（移到最上方，醒目） -->
    <div class="menu-group logout-group">
      <van-cell-group inset>
        <van-cell
            title="退出登录"
            icon="logout-o"
            clickable
            center
            @click="logout"
            class="logout-cell"
        >
          <template #right-icon>
            <van-icon name="arrow" style="font-size: 16px; color: #ee5a5a;" />
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 订单与服务 -->
    <div class="menu-group">
      <div class="menu-title">订单与服务</div>
      <van-cell-group inset>
        <van-cell title="我的订单" icon="orders-o" is-link to="/my/orders" center>
          <template #right-icon>
            <van-icon name="arrow" style="font-size: 16px;" />
          </template>
        </van-cell>
        <van-cell title="出差申请" icon="friends-o" is-link to="/my/apply" center>
          <template #right-icon>
            <van-icon name="arrow" style="font-size: 16px;" />
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 审批与设置 -->
    <div class="menu-group" v-if="hasApprovalPermission">
      <div class="menu-title">审批与设置</div>
      <van-cell-group inset>
        <!--        <van-cell title="差标设置" icon="setting-o" is-link center>
                  <template #right-icon>
                    <van-icon name="arrow" style="font-size: 16px;" />
                  </template>
                </van-cell>-->
        <van-cell title="我的审批" icon="passed" is-link to="/my/approval" center>
          <template #right-icon>
            <van-icon name="arrow" style="font-size: 16px;" />
          </template>
        </van-cell>
        <van-cell title="设置" icon="setting" is-link to="/my/settings" center>
          <template #right-icon>
            <van-icon name="arrow" style="font-size: 16px;" />
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <!-- 底部版权信息 -->
    <div class="footer">
      © 2026 智能商旅系统 - 内部使用平台
    </div>

    <!-- 底部 Tabbar -->
    <van-tabbar v-model="activeTab" route fixed placeholder>
      <van-tabbar-item icon="home-o" name="home" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="user-o" name="my" to="/my">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog } from 'vant'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('my')
const userInfo = ref<any>(null)
const hasApprovalPermission = ref(false)

const loadUserInfo = async () => {
  try {
    const res = await request.get('/users/userInfo')
    userInfo.value = res.data
    const roles = res.data?.roles || ''
    hasApprovalPermission.value = roles.includes('ROLE_AUDITOR') ||
        roles.includes('ROLE_ADMIN') ||
        roles.includes('ROLE_SUPER_ADMIN')
  } catch (err) {
    console.error('获取用户信息失败', err)
    hasApprovalPermission.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})

const logout = () => {
  showDialog({
    title: '提示',
    message: '确定要退出登录吗？',
    showCancelButton: true,
  }).then(() => {
    try {
      localStorage.removeItem('token')
      router.push('/login')
    } catch (error) {
      localStorage.removeItem('token')
      router.push('/login')
    }
  })
}
</script>

<style scoped>
.my-center {
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f7fa 0%, #ffffff 100%);
  padding-bottom: 100px; /* 给 Tabbar 留出空间 */
}

/* 用户信息卡片 */
.user-card {
  display: flex;
  align-items: center;
  padding: 32px 20px 24px;
  margin: 16px;
  background: linear-gradient(135deg, #37bb3d 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  color: #fff;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  backdrop-filter: blur(10px);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
}

.user-desc {
  font-size: 14px;
  opacity: 0.9;
}

/* 菜单分组 */
.menu-group {
  margin-bottom: 24px;
}

.menu-group:last-child {
  margin-bottom: 40px;
}

.menu-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 16px 20px 10px;
}

:deep(.van-cell-group--inset) {
  margin: 0 16px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

:deep(.van-cell) {
  padding: 16px 20px;
  font-size: 15px;
}

:deep(.van-cell__title) {
  font-size: 15px;
  font-weight: 500;
}

:deep(.van-cell::after) {
  left: 20px;
}

/* 退出登录分组（移到最上方） */
.logout-group {
  margin-bottom: 32px !important;
}

/* 退出登录单元格特殊样式 */
.logout-cell :deep(.van-cell__title) {
  color: #ee5a5a !important;
  font-weight: 600;
}

.logout-cell :deep(.van-cell__left-icon) {
  color: #ee5a5a !important;
}

/* 底部版权信息 */
.footer {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 16px;
  line-height: 1.6;
}
</style>
