<template>
  <n-space vertical size="large">
    <!-- 顶部统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16">
      <n-grid-item>
        <n-card :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <n-statistic label="总订单数" value-style="font-size: 32px; font-weight: 600; color: #10b981;">
                {{ dashboardData.totalOrders }}
              </n-statistic>
            </div>
            <n-icon size="48" color="#10b981">
              <Icon icon="mdi:cart-outline" />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <n-statistic label="待审批数" value-style="font-size: 32px; font-weight: 600; color: #f59e0b;">
                {{ dashboardData.pendingApprovals }}
              </n-statistic>
            </div>
            <n-icon size="48" color="#f59e0b">
              <Icon icon="mdi:clock-alert-outline" />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <n-statistic label="总用户数" value-style="font-size: 32px; font-weight: 600; color: #3b82f6;">
                {{ dashboardData.totalUsers }}
              </n-statistic>
            </div>
            <n-icon size="48" color="#3b82f6">
              <Icon icon="mdi:account-group-outline" />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <n-statistic label="本月订单" value-style="font-size: 32px; font-weight: 600; color: #8b5cf6;">
                {{ dashboardData.monthOrders }}
              </n-statistic>
            </div>
            <n-icon size="48" color="#8b5cf6">
              <Icon icon="mdi:calendar-month-outline" />
            </n-icon>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>

    <!-- 快捷入口 -->
    <n-card title="快捷入口" :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
      <n-grid :cols="6" :x-gap="16" :y-gap="16">
        <n-grid-item v-for="item in quickActions" :key="item.key">
          <div
              style="display: flex; flex-direction: column; align-items: center; padding: 24px 16px; cursor: pointer; border-radius: 8px; transition: all 0.3s;"
              @click="navigateTo(item.path)"
              @mouseenter="($event.currentTarget as HTMLElement).style.background = '#f3f4f6'"
              @mouseleave="($event.currentTarget as HTMLElement).style.background = 'transparent'"
          >
            <n-icon size="36" :color="item.color" style="margin-bottom: 12px;">
              <Icon :icon="item.icon" />
            </n-icon>
            <span style="font-weight: 500; color: #1f2937;">{{ item.label }}</span>
          </div>
        </n-grid-item>
      </n-grid>
    </n-card>

    <!-- 最近订单 -->
    <n-card title="最近订单" :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
      <n-data-table
          :columns="orderColumns"
          :data="recentOrders"
          :pagination="false"
          :loading="loading"
          :bordered="true"
          size="small"
      />
      <template #footer>
        <n-button type="primary" text @click="navigateTo('/order')">查看全部</n-button>
      </template>
    </n-card>

    <!-- 待审批列表 -->
    <n-card title="待审批事项" :bordered="false" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
      <n-data-table
          :columns="approvalColumns"
          :data="pendingApprovals"
          :pagination="false"
          :loading="loading"
          :bordered="true"
          size="small"
      />
      <template #footer>
        <n-button type="primary" text @click="navigateTo('/approval')">查看全部</n-button>
      </template>
    </n-card>
  </n-space>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import {
  NSpace, NGrid, NGridItem, NCard, NStatistic, NIcon,
  NDataTable, NButton, useMessage
} from 'naive-ui'
import request from '@/utils/request.ts'

const router = useRouter()
const message = useMessage()
const loading = ref(false)

// 仪表盘数据
const dashboardData = reactive({
  totalOrders: 0,
  pendingApprovals: 0,
  totalUsers: 0,
  monthOrders: 0
})

// 快捷操作
const quickActions = [
  { label: '企业管理', key: 'company', icon: 'mdi:domain', color: '#10b981', path: '/company' },
  { label: '部门管理', key: 'department', icon: 'mdi:folder-account-outline', color: '#3b82f6', path: '/department' },
  { label: '员工管理', key: 'employee', icon: 'mdi:account-tie-outline', color: '#f59e0b', path: '/employee' },
  { label: '用户管理', key: 'user', icon: 'mdi:account-group-outline', color: '#8b5cf6', path: '/user' },
  { label: '订单管理', key: 'order', icon: 'mdi:cart-outline', color: '#ef4444', path: '/order' },
  { label: '流程审批', key: 'approval', icon: 'mdi:check-decagram-outline', color: '#10b981', path: '/approval' }
]

// 最近订单表格列
const orderColumns = [
  { title: '订单号', key: 'orderNo', width: 180 },
 /* { title: '企业名称', key: 'companyName', width: 200 },*/
  { title: '申请人', key: 'username', width: 100 },
  {
    title: '总金额',
    key: 'amount',
    width: 120,
    render: (row: any) => `¥${(row.amount || 0).toFixed(2)}`
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row: any) {
      const statusMap: any = {
        'DRAFT': '草稿',
        'ORDERING': '下单中',
        'PENDING_PAYMENT': '待支付',
        'PAID': '已支付',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'REFUNDING': '退款中',
        'REFUNDED': '已退款'
      }
      return statusMap[row.status] || row.status
    }
  },
  { title: '创建时间', key: 'createTime', width: 160 }
]

// 待审批表格列
const approvalColumns = [
 /* { title: '申请单号', key: 'id', width: 80 },*/
 /* { title: '企业名称', key: 'companyName', width: 200 },*/
  { title: '申请人', key: 'applicantEmployeeName', width: 100 },
  { title: '事由', key: 'reason', width: 100, ellipsis: true },
  { title: '申请时间', key: 'createTime', width: 160 }
]

const recentOrders = ref<any[]>([])
const pendingApprovals = ref<any[]>([])

// 加载仪表盘数据
const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await request.get('/order/dashboard/global')
    if (res.code === 200) {
      dashboardData.totalOrders = res.data.totalOrders || 0
      dashboardData.pendingApprovals = res.data.pendingApprovals || 0
      dashboardData.totalUsers = res.data.totalUsers || 0
      dashboardData.monthOrders = res.data.monthOrders || 0
    }

    // 加载最近订单（分页接口，取 5 条）
    const ordersRes = await request.post('/order/page', {
      page: 1,
      size: 5
    })
    recentOrders.value = ordersRes.data?.records?.slice(0, 5) || []

    // 加载待审批列表（分页接口，取 5 条）
    const approvalsRes = await request.post('/approval/page', {
      page: 1,
      size: 5,
      auditStatus: 'PENDING'
    })
    pendingApprovals.value = approvalsRes.data?.records?.slice(0, 5) || []
  } catch (err: any) {
    message.error(`加载仪表盘失败：${err.message || '请稍后重试'}`)
  } finally {
    loading.value = false
  }
}

// 导航
const navigateTo = (path: string) => {
  router.push(path)
}

onMounted(() => {
  loadDashboard()
})
</script>

