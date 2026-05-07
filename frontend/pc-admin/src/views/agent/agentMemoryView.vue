<template>
  <n-card title="Agent 记忆管理" :bordered="false">
    <n-space vertical size="large">
      <!-- 操作栏 -->
      <n-space>
        <n-button type="primary" @click="fetchList">刷新</n-button>
        <n-button type="error" :disabled="checkedIds.length === 0" @click="batchDelete">
          批量清除 ({{ checkedIds.length }})
        </n-button>
      </n-space>

      <!-- 记忆列表表格 -->
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :row-key="(row: any) => row.userId"
        :bordered="true"
        :checked-row-keys="checkedIds"
        @update:checked-row-keys="handleCheck"
      />

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

    <!-- 记忆详情弹窗 -->
    <n-modal
      v-model:show="showDetail"
      :title="'用户记忆详情'"
      preset="dialog"
      :style="{ width: '700px', maxHeight: '80vh', overflow: 'auto' }"
    >
      <n-descriptions v-if="detail" :column="2" bordered>
        <n-descriptions-item label="用户ID">{{ detail.userId }}</n-descriptions-item>
        <n-descriptions-item label="企业ID">{{ detail.companyId || '-' }}</n-descriptions-item>
        <n-descriptions-item label="会话次数">{{ detail.sessionCount }}</n-descriptions-item>
        <n-descriptions-item label="满意度均值">{{ (detail.avgSatisfaction * 100).toFixed(1) }}%</n-descriptions-item>
        <n-descriptions-item label="最近活跃">{{ detail.lastActiveTime }}</n-descriptions-item>
        <n-descriptions-item label="上次合规">{{ detail.lastComplianceResult || '-' }}</n-descriptions-item>
      </n-descriptions>

      <n-divider v-if="detail">偏好与历史</n-divider>

      <n-space v-if="detail" vertical>
        <n-tag type="info" v-for="scene in detail.preferredScenes" :key="scene">
          {{ sceneText(scene) }}
        </n-tag>
        <div v-if="!detail.preferredScenes?.length" style="color: #999">无</div>
      </n-space>

      <n-divider v-if="detail">关注话题</n-divider>
      <n-space v-if="detail" vertical>
        <n-tag v-for="topic in detail.recentTopics" :key="topic">{{ topic }}</n-tag>
        <div v-if="!detail.recentTopics?.length" style="color: #999">无</div>
      </n-space>

      <n-divider v-if="detail">已确认政策</n-divider>
      <ul v-if="detail?.confirmedPolicies?.length">
        <li v-for="(p, i) in detail.confirmedPolicies" :key="i">{{ p }}</li>
      </ul>
      <div v-else style="color: #999">无</div>

      <template #action>
        <n-button @click="showDetail = false">关闭</n-button>
        <n-button type="error" @click="deleteOne(detail?.userId)">清除此记忆</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NModal, NDescriptions, NDescriptionsItem,
  NDivider, NTag, NPagination, useMessage, useDialog
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const dialog = useDialog()
const loading = ref(false)
const list = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const checkedIds = ref<string[]>([])
const showDetail = ref(false)
const detail = ref<any>(null)

const sceneText = (s: string) => {
  const map: Record<string, string> = { FLIGHT: '机票', HOTEL: '酒店', TRAIN: '火车票', CAR: '用车' }
  return map[s] || s
}

const columns = [
  { type: 'selection' as const },
  { title: '用户ID', key: 'userId', width: 120 },
  { title: '企业ID', key: 'companyId', width: 100 },
  { title: '会话次数', key: 'sessionCount', width: 100 },
  {
    title: '满意度',
    key: 'avgSatisfaction',
    width: 100,
    render(row: any) {
      return `${(row.avgSatisfaction * 100).toFixed(1)}%`
    },
  },
  {
    title: '偏好场景',
    key: 'preferredScenes',
    width: 200,
    render(row: any) {
      if (!row.preferredScenes?.length) return '-'
      return h(NSpace, { size: 4 }, () =>
        row.preferredScenes.map((s: string) => h(NTag, { size: 'small', type: 'info' }, () => sceneText(s)))
      )
    },
  },
  {
    title: '关注话题',
    key: 'recentTopics',
    ellipsis: true,
    width: 200,
    render(row: any) {
      return row.recentTopics?.join('、') || '-'
    },
  },
  { title: '最近活跃', key: 'lastActiveTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    align: 'center' as const,
    render(row: any) {
      return h(NSpace, { justify: 'center' }, () => [
        h(NButton, { size: 'small', type: 'primary', onClick: () => viewDetail(row) }, () => '详情'),
        h(NButton, { size: 'small', type: 'error', onClick: () => deleteOne(row.userId) }, () => '清除'),
      ])
    },
  },
]

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.post('/agent/memory/page', {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取记忆列表失败', err)
    message.error('获取记忆列表失败')
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => { currentPage.value = page; fetchList() }
const onPageSizeChange = (size: number) => { pageSize.value = size; currentPage.value = 1; fetchList() }
const handleCheck = (keys: string[]) => { checkedIds.value = keys }

const viewDetail = (row: any) => {
  detail.value = row
  showDetail.value = true
}

const deleteOne = (userId: string) => {
  if (!userId) return
  dialog.warning({
    title: '确认清除',
    content: `确定要清除用户 ${userId} 的 Agent 记忆吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/agent/memory/${userId}`)
        message.success('清除成功')
        showDetail.value = false
        fetchList()
      } catch (err) {
        message.error('清除失败')
      }
    },
  })
}

const batchDelete = () => {
  dialog.warning({
    title: '确认批量清除',
    content: `确定要清除 ${checkedIds.value.length} 条记忆吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete('/agent/memory/batch', { data: checkedIds.value })
        message.success('批量清除成功')
        checkedIds.value = []
        fetchList()
      } catch (err) {
        message.error('清除失败')
      }
    },
  })
}

onMounted(() => fetchList())
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
