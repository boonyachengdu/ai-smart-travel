<template>
  <n-card title="会话管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-select
            v-model:value="searchParams.scene"
            placeholder="场景"
            :options="sceneOptions"
            clearable
            style="width: 180px"
        />
        <n-select
            v-model:value="searchParams.status"
            placeholder="状态"
            :options="statusOptions"
            clearable
            style="width: 180px"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
      </n-space>

      <!-- 会话列表表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="sessionList"
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

    <!-- 会话消息详情弹窗 -->
    <n-modal
        v-model:show="showMessageModal"
        :title="currentSessionTitle"
        preset="dialog"
        :mask-closable="true"
        :style="{ width: '800px', maxHeight: '80vh', overflow: 'auto' }"
    >
      <n-timeline>
        <n-timeline-item
            v-for="msg in messages"
            :key="msg.id"
            :type="msg.role === 'user' ? 'success' : 'primary'"
            :title="msg.role === 'user' ? '用户' : '助手'"
        >
          <div style="white-space: pre-wrap; word-break: break-all;">
            {{ msg.content }}
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 4px;">
            {{ msg.createTime }}
          </div>
        </n-timeline-item>
      </n-timeline>

      <template #action>
        <n-button @click="showMessageModal = false">关闭</n-button>
        <n-button type="error" @click="deleteSession(currentSessionId)">删除会话</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInputNumber, NSelect,
  NModal, NTimeline, NTimelineItem, NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showMessageModal = ref(false)
const currentSessionId = ref<number | null>(null)
const currentSessionTitle = ref('会话详情')
const messages = ref<any[]>([])

// 搜索参数
const searchParams = reactive({
  userId: null as number | null,
  companyId: null as number | null,
  scene: null as string | null,
  status: null as string | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const sessionList = ref<any[]>([])

// 场景下拉选项（可从常量或接口动态获取）
const sceneOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
  { label: '用车', value: 'CAR' },
  { label: '通用', value: 'DEFAULT' },
  { label: '问答', value: 'QA' }
]

// 状态下拉选项
const statusOptions = [
  { label: '活跃', value: 'ACTIVE' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已归档', value: 'ARCHIVED' }
]

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 200 },
  { title: '公司名称', key: 'companyName', width: 200 },
  { title: '用户名称', key: 'username', width: 120 },
  { title: '场景', key: 'scene', width: 100 },
  { title: '标题', key: 'title', ellipsis: true, width: 240 },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render(row: any) {
      const map: any = { ACTIVE: '活跃', CLOSED: '已关闭', ARCHIVED: '已归档' }
      return map[row.status] || row.status
    }
  },
  { title: '最后活跃', key: 'lastActiveTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    align: 'center',
    render(row: any) {
      return h(
          NButton,
          { size: 'small', type: 'primary', onClick: () => viewMessages(row) },
          { default: () => '查看消息' }
      )
    }
  }
]

// 获取会话列表
const fetchSessions = async () => {
  loading.value = true
  try {
    const res = await request.post('/session/page', {
      page: currentPage.value,
      size: pageSize.value,
      userId: searchParams.userId,
      companyId: searchParams.companyId,
      scene: searchParams.scene,
      status: searchParams.status
    })
    sessionList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: sessionList.value.length
    })
  } catch (err) {
    console.error('获取会话列表失败', err)
    message.error('获取会话列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchSessions()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchSessions()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchSessions()
}

// 查看会话消息
const viewMessages = async (row: any) => {
  currentSessionId.value = row.id
  currentSessionTitle.value = row.title || `会话 ${row.id}`
  try {
    const res = await request.get(`/session/${row.id}/messages`)
    messages.value = res.data || []
    showMessageModal.value = true
  } catch (err) {
    console.error('加载消息失败', err)
    message.error('加载消息失败')
  }
}

// 删除会话
const deleteSession = async (id: number | null) => {
  if (!id) return
  try {
    await request.delete(`/session/${id}`)
    message.success('删除成功')
    fetchSessions()
    showMessageModal.value = false
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

// 重置搜索
const resetSearch = () => {
  searchParams.userId = null
  searchParams.companyId = null
  searchParams.scene = null
  searchParams.status = null
  currentPage.value = 1
  fetchSessions()
}

onMounted(() => {
  fetchSessions()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>