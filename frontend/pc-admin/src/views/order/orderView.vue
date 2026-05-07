<template>
  <n-card title="订单管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.orderNo" placeholder="订单号" clearable style="width: 180px" />
        <n-select
            v-model:value="searchParams.orderType"
            placeholder="订单类型"
            :options="typeOptions"
            clearable
            style="width: 180px"
        />
        <n-select
            v-model:value="searchParams.status"
            placeholder="订单状态"
            :options="statusOptions"
            clearable
            style="width: 180px"
        />
        <n-select
            v-model:value="searchParams.auditStatus"
            placeholder="审核状态"
            :options="auditStatusOptions"
            clearable
            style="width: 180px"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="orderList"
          :loading="loading"
          :row-key="(row: any) => row.id"
          :bordered="true"
      />

      <!-- 独立的分页组件 -->
      <div class="pagination-wrapper">
        <n-pagination
            v-model:page="currentPage"
            v-model:page-size="pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            @update:page="onPageChange"
            @update:page-size="onPageSizeChange"
        />
      </div>
    </n-space>

    <!-- 订单详情弹窗 -->
    <n-modal
        v-model:show="showDetailModal"
        :title="`订单详情 - ${currentOrder?.orderNo || ''}`"
        preset="dialog"
        :mask-closable="true"
        :style="{ width: '900px', maxHeight: '85vh', overflow: 'auto' }"
    >
      <n-descriptions :column="2" bordered>
        <n-descriptions-item label="订单号">{{ currentOrder?.orderNo }}</n-descriptions-item>
        <n-descriptions-item label="用户名">{{ currentOrder?.username }}</n-descriptions-item>
        <n-descriptions-item label="类型">{{ getOrderTypeLabel(currentOrder?.orderType) }}</n-descriptions-item>
        <n-descriptions-item label="金额">¥ {{ currentOrder?.amount?.toFixed(2) }}</n-descriptions-item>
        <n-descriptions-item label="订单状态">{{ getOrderStatusLabel(currentOrder?.status) }}</n-descriptions-item>
        <n-descriptions-item label="审核状态">{{ getAuditStatusLabel(currentOrder?.auditStatus) }}</n-descriptions-item>
        <n-descriptions-item label="创建时间">{{ currentOrder?.createTime }}</n-descriptions-item>
        <n-descriptions-item label="支付时间">{{ currentOrder?.payTime || '未支付' }}</n-descriptions-item>
        <n-descriptions-item label="支付方式">{{ currentOrder?.payType || '-' }}</n-descriptions-item>
      </n-descriptions>

      <!-- 行程信息（如果有 journey JSONB，可渲染） -->
      <n-card title="行程信息" size="small" style="margin-top: 16px;">
        <p v-if="!currentOrder?.journey">暂无行程信息</p>
        <pre v-else style="background: #f8f9fa; padding: 12px; border-radius: 8px;">
          {{ JSON.stringify(currentOrder.journey, null, 2) }}
        </pre>
      </n-card>

      <template #action>
        <n-button @click="showDetailModal = false">关闭</n-button>
        <n-button
            v-if="currentOrder?.auditStatus === 'PENDING' && hasAuditPermission"
            type="warning"
            @click="openAuditModal"
        >
          审核
        </n-button>
        <n-button type="error" @click="deleteOrder(currentOrder?.id)">删除订单</n-button>
      </template>
    </n-modal>

    <!-- 审核弹窗 -->
    <n-modal
        v-model:show="showAuditModal"
        title="订单审核"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '500px' }"
    >
      <n-form ref="auditFormRef" :model="auditForm" :rules="auditRules">
        <n-descriptions :column="1" bordered>
          <n-descriptions-item label="订单号">{{ currentOrder?.orderNo }}</n-descriptions-item>
          <n-descriptions-item label="申请人">{{ currentOrder?.username }}</n-descriptions-item>
          <n-descriptions-item label="金额">¥ {{ currentOrder?.amount?.toFixed(2) }}</n-descriptions-item>
          <n-descriptions-item label="订单类型">{{ getOrderTypeLabel(currentOrder?.orderType) }}</n-descriptions-item>
        </n-descriptions>

        <n-form-item label="审核意见" path="remark" style="margin-top: 16px;">
          <n-input
              v-model:value="auditForm.remark"
              type="textarea"
              placeholder="请输入审核意见（必填）"
              :rows="4"
              maxlength="500"
              show-count
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showAuditModal = false">取消</n-button>
        <n-button type="success" @click="handleApprove" :loading="approving">通过</n-button>
        <n-button type="error" @click="handleReject" :loading="rejecting">拒绝</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal,
  NDescriptions, NDescriptionsItem, NSelect, NPagination, useMessage, NForm, NFormItem
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showDetailModal = ref(false)
const showAuditModal = ref(false)
const currentOrder = ref<any>(null)
const auditFormRef = ref(null)
const approving = ref(false)
const rejecting = ref(false)
const hasAuditPermission = ref(false)

// 审核表单
const auditForm = reactive({
  remark: ''
})

// 审核表单验证规则
const auditRules = {
  remark: {
    required: true,
    message: '请输入审核意见',
    trigger: 'blur'
  }
}

// 搜索参数
const searchParams = reactive({
  orderNo: '',
  orderType: null as string | null,
  status: null as string | null,
  auditStatus: null as string | null,
  userId: null as number | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const orderList = ref<any[]>([])

// 订单类型下拉选项
const typeOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
  { label: '用车', value: 'CAR' }
]

// 订单状态下拉选项
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '下单中', value: 'ORDERING' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '已支付', value: 'PAID' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' }
]

// 审核状态下拉选项
const auditStatusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '通过', value: 'APPROVED' },
  { label: '拒绝', value: 'REJECTED' }
]

// 从表格打开审核弹窗
const openAuditModalFromTable = (row: any) => {
  currentOrder.value = row
  auditForm.remark = ''
  showAuditModal.value = true
}

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '企业名称', key: 'companyName', width: 200 },
  { title: '订单号', key: 'orderNo', width: 180 },
  {
    title: '类型',
    key: 'orderType',
    width: 120,
    render(row: any) {
      const option = typeOptions.find(opt => opt.value === row.orderType)
      return option ? option.label : row.orderType
    }
  },
  {
    title: '金额',
    key: 'amount',
    width: 120,
    render(row: any) {
      return `¥ ${row.amount?.toFixed(2) || 0}`
    }
  },
  {
    title: '订单状态',
    key: 'status',
    width: 120,
    render(row: any) {
      const option = statusOptions.find(opt => opt.value === row.status)
      return option ? option.label : row.status
    }
  },
  {
    title: '审核状态',
    key: 'auditStatus',
    width: 120,
    render(row: any) {
      const option = auditStatusOptions.find(opt => opt.value === row.auditStatus)
      return option ? option.label : row.auditStatus
    }
  },
  { title: '用户名', key: 'username', width: 120 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    align: 'center',
    render(row: any) {
      const buttons = []

      // 详情按钮
      buttons.push(
          h(
              NButton,
              { size: 'small', type: 'primary', onClick: () => viewDetail(row) },
              { default: () => '详情' }
          )
      )

      // 审核按钮（仅待审批且有权限时显示）
      if (row.auditStatus === 'PENDING' && hasAuditPermission.value) {
        buttons.push(
            h(
                NButton,
                { size: 'small', type: 'warning', onClick: () => openAuditModalFromTable(row) },
                { default: () => '审核' }
            )
        )
      }

      // 使用 h 片段包裹多个按钮
      return h('div', { style: { display: 'flex', gap: '8px', justifyContent: 'center' } }, buttons)
    }
  }
]

// 获取订单类型中文标签
const getOrderTypeLabel = (type: string) => {
  if (!type) return '-'
  const option = typeOptions.find(opt => opt.value === type)
  return option ? option.label : type
}

// 获取订单状态中文标签
const getOrderStatusLabel = (status: string) => {
  if (!status) return '-'
  const option = statusOptions.find(opt => opt.value === status)
  return option ? option.label : status
}

// 获取审核状态中文标签
const getAuditStatusLabel = (auditStatus: string) => {
  if (!auditStatus) return '-'
  const option = auditStatusOptions.find(opt => opt.value === auditStatus)
  return option ? option.label : auditStatus
}

// 检查用户审核权限
const checkAuditPermission = async () => {
  try {
    const res = await request.get('/users/userInfo')
    const userInfo = res.data
    const roles = userInfo?.roles || ''
    // 检查是否有 ROLE_AUDITOR 角色
    hasAuditPermission.value = roles.includes('ROLE_AUDITOR') || roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SUPER_ADMIN')
  } catch (err) {
    console.error('获取用户信息失败', err)
    hasAuditPermission.value = false
  }
}

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.post('/order/page', {
      page: currentPage.value,
      size: pageSize.value,
      orderNo: searchParams.orderNo,
      orderType: searchParams.orderType,
      status: searchParams.status,
      auditStatus: searchParams.auditStatus,
      userId: searchParams.userId
    })
    orderList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: orderList.value.length
    })
  } catch (err) {
    console.error('获取订单列表失败', err)
    message.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchOrders()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchOrders()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchOrders()
}

// 查看订单详情
const viewDetail = (row: any) => {
  currentOrder.value = row
  showDetailModal.value = true
}

// 打开审核弹窗
const openAuditModal = () => {
  auditForm.remark = ''
  showAuditModal.value = true
}

// 获取审批 ID（如果 approvalId 为空则使用订单 ID）
const getApprovalId = () => {
  return currentOrder.value?.approvalId || currentOrder.value?.id
}

// 审核通过
const handleApprove = async () => {
  if (!auditForm.remark.trim()) {
    message.error('请输入审核意见')
    return
  }

  approving.value = true
  try {
    const approvalId = getApprovalId()
    if (!approvalId) {
      message.error('审批 ID 不存在')
      return
    }
    await request.post(`/order/approve/${approvalId}`, {
      remark: auditForm.remark
    })
    message.success('审核通过')
    showAuditModal.value = false
    showDetailModal.value = false
    fetchOrders()
  } catch (err) {
    console.error('审核通过失败', err)
    message.error('审核通过失败')
  } finally {
    approving.value = false
  }
}

// 审核拒绝
const handleReject = async () => {
  if (!auditForm.remark.trim()) {
    message.error('请输入审核意见')
    return
  }

  rejecting.value = true
  try {
    const approvalId = getApprovalId()
    if (!approvalId) {
      message.error('审批 ID 不存在')
      return
    }
    await request.post(`/order/reject/${approvalId}`, {
      reason: auditForm.remark
    })
    message.success('审核拒绝')
    showAuditModal.value = false
    showDetailModal.value = false
    fetchOrders()
  } catch (err) {
    console.error('审核拒绝失败', err)
    message.error('审核拒绝失败')
  } finally {
    rejecting.value = false
  }
}

// 删除订单
const deleteOrder = async (id: number) => {
  if (!id) return
  try {
    await request.delete(`/order/${id}`)
    message.success('删除成功')
    fetchOrders()
    showDetailModal.value = false
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

// 重置搜索
const resetSearch = () => {
  searchParams.orderNo = ''
  searchParams.orderType = null
  searchParams.status = null
  searchParams.auditStatus = null
  searchParams.userId = null
  currentPage.value = 1
  fetchOrders()
}

onMounted(() => {
  checkAuditPermission()
  fetchOrders()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
