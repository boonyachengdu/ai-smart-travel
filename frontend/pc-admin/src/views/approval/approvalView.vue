<template>
  <n-card title="流程审批管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.orderNo" placeholder="订单号" clearable style="width: 180px" />
        <n-select
            v-model:value="searchParams.status"
            placeholder="审批状态"
            :options="statusOptions"
            clearable
            style="width: 180px"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="success" @click="openAddModal">新增审批</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="approvalList"
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

    <!-- 审批弹窗（通过/拒绝） -->
    <n-modal
        v-model:show="showApproveModal"
        title="审批操作"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '500px' }"
    >
      <n-form ref="approveFormRef" :model="approveForm">
        <n-form-item label="审批意见" path="remark" label-width="100">
          <n-input
              v-model:value="approveForm.remark"
              type="textarea"
              :rows="4"
              placeholder="请输入审批意见（选填）"
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showApproveModal = false">取消</n-button>
        <n-button type="error" @click="approve('拒绝')">拒绝</n-button>
        <n-button type="success" @click="approve('通过')">通过</n-button>
      </template>
    </n-modal>

    <!-- 出行项详情弹窗 -->
    <n-modal
        v-model:show="showTripItemsModal"
        title="出行项详情"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '900px' }"
    >
      <n-data-table
          :columns="tripItemColumns"
          :data="currentTripItems"
          :pagination="false"
          :bordered="true"
          size="small"
      />
    </n-modal>

    <!-- 新增审批单弹窗 -->
    <n-modal
        v-model:show="showAddModal"
        title="新增审批单"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '700px', maxHeight: '80vh', overflow: 'auto' }"
    >
      <div style="max-height: 60vh; overflow-y: auto; padding-right: 10px;">
        <n-form ref="addFormRef" :model="addForm" :rules="addFormRules">
          <n-form-item label="出差事由" path="reason" label-width="100">
            <n-input
                v-model:value="addForm.reason"
                type="textarea"
                :rows="3"
                placeholder="请输入出差事由"
            />
          </n-form-item>

          <n-divider>出行项列表</n-divider>

          <n-space vertical size="medium">
            <n-button type="primary" @click="addTripItem" style="width: 150px">
              添加出行项
            </n-button>

            <template v-for="(item, index) in addForm.tripItems" :key="index">
              <n-card :bordered="true" embedded>
                <template #header>
                  <n-space justify="space-between">
                    <n-tag :type="getTripTypeColor(item.tripType)">
                      {{ getTripTypeName(item.tripType) }}
                    </n-tag>
                    <n-button type="error" size="small" @click="removeTripItem(index)">删除</n-button>
                  </n-space>
                </template>

                <n-grid :cols="2" :x-gap="12" :y-gap="8">
                  <n-form-item label="出行项名称" label-width="100">
                    <n-input v-model:value="item.tripName" placeholder="如：北京到上海航班" />
                  </n-form-item>
                  <n-form-item label="出行类型" label-width="100">
                    <n-select
                        v-model:value="item.tripType"
                        :options="tripTypeOptions"
                        placeholder="请选择"
                    />
                  </n-form-item>
                </n-grid>

                <n-grid :cols="2" :x-gap="12" :y-gap="8">
                  <n-form-item label="出发地" label-width="100">
                    <n-input v-model:value="item.departure" placeholder="请输入出发地" />
                  </n-form-item>
                  <n-form-item label="目的地" label-width="100">
                    <n-input v-model:value="item.arrival" placeholder="请输入目的地" />
                  </n-form-item>
                </n-grid>

                <n-grid :cols="2" :x-gap="12" :y-gap="8">
                  <n-form-item label="开始时间" label-width="100">
                    <n-date-picker
                        v-model:value="item.startTime"
                        type="datetime"
                        placeholder="请选择开始日期时间"
                        style="width: 100%"
                        value-format="yyyy-MM-dd HH:mm:ss"
                    />
                  </n-form-item>
                  <n-form-item label="结束时间" label-width="100">
                    <n-date-picker
                        v-model:value="item.endTime"
                        type="datetime"
                        placeholder="请选择结束日期时间"
                        style="width: 100%"
                        value-format="yyyy-MM-dd HH:mm:ss"
                    />
                  </n-form-item>
                </n-grid>

                <n-grid :cols="2" :x-gap="12" :y-gap="8">
                  <n-form-item label="预估金额" label-width="100">
                    <n-input-number
                        v-model:value="item.estimatedAmount"
                        placeholder="0"
                        :min="0"
                        style="width: 100%"
                    />
                  </n-form-item>
                  <n-form-item label="实际金额" label-width="100">
                    <n-input-number
                        v-model:value="item.actualAmount"
                        placeholder="0"
                        :min="0"
                        style="width: 100%"
                    />
                  </n-form-item>
                </n-grid>
              </n-card>
            </template>
          </n-space>
        </n-form>
      </div>

      <template #action>
        <n-button @click="showAddModal = false">取消</n-button>
        <n-button type="primary" @click="submitAddForm">提交</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal, NForm,
  NFormItem, NSelect, useMessage, NTag, NGrid, NDatePicker,
  NDivider, NInputNumber, NPagination
} from 'naive-ui'
import request from '@/utils/request.ts'

const message = useMessage()
const loading = ref(false)
const showApproveModal = ref(false)
const showTripItemsModal = ref(false)
const showAddModal = ref(false)
const approveFormRef = ref<any>(null)
const addFormRef = ref<any>(null)
const approveCurrentId = ref<number | null>(null)
const currentTripItems = ref<any[]>([])

// 搜索参数
const searchParams = reactive({
  orderNo: null as string | null,
  status: null as string | null,
  applicantEmployeeName: null as string | null,
  approverEmployeeName: null as string | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const approvalList = ref<any[]>([])

// 审批状态下拉选项
const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '通过', value: 'APPROVED' },
  { label: '拒绝', value: 'REJECTED' }
]

// 出行类型选项
const tripTypeOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
  { label: '用车', value: 'CAR' }
]

// 获取出行类型名称
const getTripTypeName = (type: string) => {
  const map: any = {
    FLIGHT: '机票',
    HOTEL: '酒店',
    TRAIN: '火车',
    CAR: '用车'
  }
  return map[type] || type
}

// 获取出行类型标签颜色
const getTripTypeColor = (type: string) => {
  const map: any = {
    FLIGHT: 'info',
    HOTEL: 'warning',
    TRAIN: 'success',
    CAR: 'error'
  }
  return map[type] || 'default'
}

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '企业名称', key: 'companyName', width: 180, render: (row: any) => row.companyName || '-' },
  { title: '订单号', key: 'orderNo', width: 180, render: (row: any) => row.orderNo || '-' },
  { title: '申请人', key: 'applicantEmployeeName', width: 100, render: (row: any) => row.applicantEmployeeName || '-' },
  { title: '审批人', key: 'approverEmployeeName', width: 100, render: (row: any) => row.approverEmployeeName || '-' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row: any) {
      const map: any = {
        PENDING: '待审批',
        APPROVED: '通过',
        REJECTED: '拒绝'
      }
      const typeMap: any = {
        PENDING: 'warning',
        APPROVED: 'success',
        REJECTED: 'error'
      }
      return h(
          NTag,
          { type: typeMap[row.status] || 'default', size: 'small' },
          { default: () => map[row.status] || row.status }
      )
    }
  },
  {
    title: '总预估金额',
    key: 'totalEstimatedAmount',
    width: 120,
    render(row: any) {
      return `¥${(row.totalEstimatedAmount || 0).toFixed(2)}`
    }
  },
  {
    title: '总实际金额',
    key: 'totalActualAmount',
    width: 120,
    render(row: any) {
      return `¥${(row.totalActualAmount || 0).toFixed(2)}`
    }
  },
  {
    title: '出行项数',
    key: 'tripItems',
    width: 100,
    render(row: any) {
      const count = row.tripItems?.length || 0
      return h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            text: true,
            onClick: () => openTripItemsModal(row.tripItems || [])
          },
          { default: () => `${count} 项详情` }
      )
    }
  },
  { title: '审批意见', key: 'remark', ellipsis: true, width: 200 },
  { title: '创建时间', key: 'createTime', width: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    align: 'center',
    render(row: any) {
      // 只有待审批状态才显示审批按钮
      if (row.status !== 'PENDING') {
        return h('span', { style: { color: '#999' } }, '已审批')
      }
      return h(
          NButton,
          {
            size: 'small',
            type: 'primary',
            onClick: () => openApproveModal(row.id)
          },
          { default: () => '审批' }
      )
    }
  }
]

// 出行项详情表格列
const tripItemColumns = [
  {
    title: '类型',
    key: 'tripType',
    width: 80,
    render(row: any) {
      return h(
          NTag,
          { type: getTripTypeColor(row.tripType), size: 'small' },
          { default: () => getTripTypeName(row.tripType) }
      )
    }
  },
  { title: '出行项名称', key: 'tripName', width: 180 },
  { title: '出发地', key: 'departure', width: 100 },
  { title: '目的地', key: 'arrival', width: 100 },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '结束时间', key: 'endTime', width: 160 },
  {
    title: '预估金额',
    key: 'estimatedAmount',
    width: 100,
    render(row: any) {
      return `¥${(row.estimatedAmount || 0).toFixed(2)}`
    }
  },
  {
    title: '实际金额',
    key: 'actualAmount',
    width: 100,
    render(row: any) {
      return `¥${(row.actualAmount || 0).toFixed(2)}`
    }
  },
  { title: '事由', key: 'reason', ellipsis: true, width: 200, render: (row: any) => row.reason || '-' }
]

// 审批表单数据
const approveForm = reactive({
  status: '',
  remark: ''
})

// 新增审批单表单
const createEmptyTripItem = () => ({
  tripType: null,
  tripName: '',
  departure: '',
  arrival: '',
  startTime: null,
  endTime: null,
  estimatedAmount: 0,
  actualAmount: 0
})

const addForm = reactive({
  reason: '',
  tripItems: [] as any[]
})

const addFormRules = {
  reason: { required: true, message: '请输入出差事由', trigger: 'blur' }
}

// 打开审批弹窗
const openApproveModal = (id: number) => {
  approveCurrentId.value = id
  approveForm.remark = ''
  showApproveModal.value = true
}

// 打开出行项详情弹窗
const openTripItemsModal = (tripItems: any[]) => {
  currentTripItems.value = tripItems || []
  showTripItemsModal.value = true
}

// 打开新增弹窗
const openAddModal = () => {
  addForm.reason = ''
  addForm.tripItems = [createEmptyTripItem()]
  addFormRef.value?.restoreValidation()
  showAddModal.value = true
}

// 添加出行项
const addTripItem = () => {
  addForm.tripItems.push(createEmptyTripItem())
}

// 删除出行项
const removeTripItem = (index: number) => {
  if (addForm.tripItems.length === 1) {
    message.warning('至少保留一个出行项')
    return
  }
  addForm.tripItems.splice(index, 1)
}

// 执行审批
const approve = async (action: '通过' | '拒绝') => {
  if (!approveCurrentId.value) return

  try {
    const payload = {
      status: action === '通过' ? 'APPROVED' : 'REJECTED',
      remark: approveForm.remark || ''
    }
    await request.put(`/approval/${approveCurrentId.value}/approve`, payload)
    message.success(`已${action}`)
    showApproveModal.value = false
    fetchApprovals()
  } catch (err: any) {
    message.error(`${action}失败：${err.message || '请稍后重试'}`)
  }
}

// 提交新增表单
const submitAddForm = async () => {
  try {
    await addFormRef.value?.validate()

    if (addForm.tripItems.length === 0) {
      message.warning('请至少添加一个出行项')
      return
    }

    // 验证每个出行项的必填字段
    for (let i = 0; i < addForm.tripItems.length; i++) {
      const item = addForm.tripItems[i]
      if (!item.tripType || !item.tripName || !item.departure || !item.arrival || !item.startTime || !item.endTime) {
        message.error(`请完整填写第 ${i + 1} 个出行项的信息`)
        return
      }
    }

    // TODO: 从用户状态中获取实际的 companyId 和 applicantId
    const currentUser = JSON.parse(localStorage.getItem('userInfo') || '{}')

    // 构建符合后端接口的数据结构
    const payload = {
      approval: {
        reason: addForm.reason,
        companyId: currentUser.companyId || 1,
        applicantId: currentUser.id || 1,
        totalEstimatedAmount: addForm.tripItems.reduce((sum, item) => sum + (Number(item.estimatedAmount) || 0), 0),
        status: 'PENDING'
      },
      tripItems: addForm.tripItems.map(item => ({
        tripType: item.tripType,
        tripName: item.tripName,
        departure: item.departure,
        arrival: item.arrival,
        startTime: item.startTime,
        endTime: item.endTime,
        estimatedAmount: Number(item.estimatedAmount) || 0,
        actualAmount: Number(item.actualAmount) || 0,
        reason: addForm.reason,
        companyId: currentUser.companyId || 1
      }))
    }

    await request.post('/approval/create-with-trips', payload)
    message.success('新增成功')
    showAddModal.value = false
    fetchApprovals()
  } catch (err: any) {
    if (err?.errors) {
      message.error('请填写完整信息')
    } else {
      message.error(`新增失败：${err.message || '请稍后重试'}`)
    }
  }
}

// 获取审批列表
const fetchApprovals = async () => {
  loading.value = true
  try {
    const res = await request.post('/approval/page', {
      page: currentPage.value,
      size: pageSize.value,
      orderNo: searchParams.orderNo,
      status: searchParams.status
    })
    approvalList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: approvalList.value.length
    })
  } catch (err: any) {
    message.error(`获取审批列表失败：${err.message || '请稍后重试'}`)
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchApprovals()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchApprovals()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchApprovals()
}

// 重置搜索
const resetSearch = () => {
  searchParams.orderNo = null
  searchParams.status = null
  searchParams.applicantEmployeeName = null
  searchParams.approverEmployeeName = null
  currentPage.value = 1
  fetchApprovals()
}

onMounted(() => {
  fetchApprovals()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>