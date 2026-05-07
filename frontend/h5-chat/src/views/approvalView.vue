<template>
  <div class="approval-list">
    <van-nav-bar title="我的审批" left-arrow @click-left="goBack" />

    <van-tabs v-model:active="activeTab" animated sticky offset-top="46px" @change="onTabChange">
      <van-tab title="待审批" name="pending">
        <div class="approval-list-container">
          <van-loading v-if="loading.pending" type="spinner" size="24" />
          <van-empty v-else-if="!loading.pending && pendingList.length === 0" description="暂无待审批" />
          <van-list
              v-else
              v-model:loading="loading.pending"
              :finished="finished.pending"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('pending')"
          >
            <div v-for="item in pendingList" :key="item.id" class="approval-card">
              <div class="approval-header">
                <span class="approval-no">申请号：{{ item.orderNo || 'N/A' }}</span>
                <span :class="['approval-status', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
              </div>
              <div class="approval-info">
                <div class="info-row">
                  <span class="label">申请人：</span>
                  <span class="value">{{ item.applicantName || '未知' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">出差事由：</span>
                  <span class="value reason">{{ item.reason || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">总预估金额：</span>
                  <span class="value amount">¥{{ (item.totalEstimatedAmount || 0).toFixed(2) }}</span>
                </div>
                <div class="info-row">
                  <span class="label">出行项数：</span>
                  <span class="value">{{ item.tripItems?.length || 0 }} 项</span>
                </div>
                <div class="info-row">
                  <span class="label">申请时间：</span>
                  <span class="value">{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
              <div class="approval-footer">
                <van-button type="primary" size="small" block @click="openApproveModal(item)">
                  审批
                </van-button>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>

      <van-tab title="已审批" name="approved">
        <div class="approval-list-container">
          <van-loading v-if="loading.approved" type="spinner" size="24" />
          <van-empty v-else-if="!loading.approved && approvedList.length === 0" description="暂无已审批" />
          <van-list
              v-else
              v-model:loading="loading.approved"
              :finished="finished.approved"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('approved')"
          >
            <div v-for="item in approvedList" :key="item.id" class="approval-card">
              <div class="approval-header">
                <span class="approval-no">申请号：{{ item.orderNo || 'N/A' }}</span>
                <span :class="['approval-status', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
              </div>
              <div class="approval-info">
                <div class="info-row">
                  <span class="label">申请人：</span>
                  <span class="value">{{ item.applicantEmployeeName || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">出差事由：</span>
                  <span class="value reason">{{ item.reason || '-' }}</span>
                </div>
<!--                <div class="info-row">-->
<!--                  <span class="label">总预估金额：</span>-->
<!--                  <span class="value amount">¥{{ (item.totalEstimatedAmount || 0).toFixed(2) }}</span>-->
<!--                </div>-->
                <div class="info-row">
                  <span class="label">审批人：</span>
                  <span class="value">{{ item.approverEmployeeName || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">审批意见：</span>
                  <span class="value">{{ item.remark || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">申请时间：</span>
                  <span class="value">{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>
    </van-tabs>

    <!-- 审批弹窗 -->
    <van-dialog
        v-model:show="showApproveModal"
        title="审批"
        show-cancel-button
        confirm-button-text="通过"
        cancel-button-text="拒绝"
        @confirm="handleApprove('APPROVED')"
        @cancel="handleReject('REJECTED')"
    >
      <van-field
          v-model="approveForm.remark"
          rows="3"
          autosize
          type="textarea"
          placeholder="请输入审批意见（选填）"
          style="padding: 16px;"
      />
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request'

interface ApprovalItem {
  id: number
  orderNo: string
  applicantId: number
  applicantEmployeeName?: string
  approverEmployeeName?: string
  status: string
  reason: string
  remark: string
/*  totalEstimatedAmount: number*/
  tripItems?: any[]
  createTime: string
}

const activeTab = ref('pending')
const router = useRouter()

const pendingList = ref<ApprovalItem[]>([])
const approvedList = ref<ApprovalItem[]>([])

const loading = ref({
  pending: false,
  approved: false
})

const finished = ref({
  pending: false,
  approved: false
})

const pagination = ref({
  pending: { page: 1, size: 10 },
  approved: { page: 1, size: 10 }
})

const showApproveModal = ref(false)
const currentApproval = ref<ApprovalItem | null>(null)
const approveForm = reactive({
  remark: ''
})

const goBack = () => {
  router.back()
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'pending',
    'APPROVED': 'approved',
    'REJECTED': 'rejected'
  }
  return map[status] || 'pending'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return map[status] || status
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const loadApprovals = async (type: string, isLoadMore = false) => {
  const key = type as keyof typeof loading
  const paginationKey = type as keyof typeof pagination

  loading.value[key] = true

  try {
    const currentPage = pagination.value[paginationKey].page
    const pageSize = pagination.value[paginationKey].size

    const status = type === 'pending' ? 'PENDING' : undefined

    const res = await request.post('/approval/page', {
      page: currentPage,
      size: pageSize,
      status: status
    })

    const approvals = res.data?.records || []

    if (isLoadMore) {
      if (type === 'pending') {
        pendingList.value = [...pendingList.value, ...approvals]
      } else {
        approvedList.value = [...approvedList.value, ...approvals]
      }
    } else {
      if (type === 'pending') {
        pendingList.value = approvals
      } else {
        approvedList.value = approvals
      }
    }

    if (approvals.length < pageSize) {
      finished.value[key] = true
    } else {
      finished.value[key] = false
      pagination.value[paginationKey].page = currentPage + 1
    }
  } catch (err: any) {
    console.error('加载审批列表失败', err)
    showToast(err.response?.data?.message || '加载失败')
    finished.value[key] = true
  } finally {
    loading.value[key] = false
  }
}

const onLoadMore = (type: string) => {
  loadApprovals(type, true)
}

const onTabChange = (name: string) => {
  const type = name as string
  const paginationKey = type as keyof typeof pagination

  pagination.value[paginationKey] = { page: 1, size: 10 }
  finished.value[type as keyof typeof finished] = false

  loadApprovals(type, false)
}

const openApproveModal = (item: ApprovalItem) => {
  currentApproval.value = item
  approveForm.remark = ''
  showApproveModal.value = true
}

const handleApprove = async (result: string) => {
  if (!currentApproval.value) return

  try {
    await request.put(`/approval/${currentApproval.value.id}/approve`, {
      id: currentApproval.value.id,
      status: result,
      remark: approveForm.remark
    })

    showSuccessToast('审批通过')

    const index = pendingList.value.findIndex(item => item.id === currentApproval.value!.id)
    if (index !== -1) {
      pendingList.value.splice(index, 1)
    }

    pagination.value.pending = { page: 1, size: 10 }
    finished.value.pending = false
    await loadApprovals('pending', false)
  } catch (err: any) {
    console.error('审批失败', err)
    showToast(err.response?.data?.message || '审批失败')
  }
}

const handleReject = async (result: string) => {
  if (!currentApproval.value) return

  try {
    await request.put(`/approval/${currentApproval.value.id}/approve`, {
      id: currentApproval.value.id,
      status: result,
      remark: approveForm.remark
    })

    showSuccessToast('已拒绝')

    const index = pendingList.value.findIndex(item => item.id === currentApproval.value!.id)
    if (index !== -1) {
      pendingList.value.splice(index, 1)
    }

    pagination.value.pending = { page: 1, size: 10 }
    finished.value.pending = false
    await loadApprovals('pending', false)
  } catch (err: any) {
    console.error('审批失败', err)
    showToast(err.response?.data?.message || '审批失败')
  }
}

onMounted(() => {
  loadApprovals('pending', false)
})
</script>

<style scoped>
.approval-list {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 100px;
}

.approval-list-container {
  padding: 16px;
  min-height: calc(100vh - 100px);
}

.approval-card {
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.approval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.approval-no {
  font-size: 14px;
  color: #333;
  font-weight: 600;
}

.approval-status {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 16px;
  font-weight: 500;
}

.pending { background: #fff7e6; color: #fa8c16; }
.approved { background: #f0f9eb; color: #52c41a; }
.rejected { background: #fff1f0; color: #ff4d4f; }

.approval-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  color: #999;
  width: 90px;
  flex-shrink: 0;
}

.value {
  color: #333;
  flex: 1;
}

.value.reason {
  white-space: pre-wrap;
  word-break: break-all;
}

.value.amount {
  color: #ee0a24;
  font-weight: 600;
}

.approval-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

:deep(.van-dialog) {
  overflow: visible;
}

:deep(.van-dialog__message) {
  padding: 0;
}
</style>
