<template>
  <n-card title="Agent 反馈分析" :bordered="false">
    <n-space vertical size="large">
      <!-- 统计卡片 -->
      <n-space>
        <n-card size="small" :bordered="false" style="background: #f0fdf4; min-width: 140px;">
          <n-statistic label="反馈总数" :value="stats.total" />
        </n-card>
        <n-card size="small" :bordered="false" style="background: #eff6ff; min-width: 140px;">
          <n-statistic label="正面反馈" :value="stats.positive" />
        </n-card>
        <n-card size="small" :bordered="false" style="background: #fef2f2; min-width: 140px;">
          <n-statistic label="负面反馈" :value="stats.negative" />
        </n-card>
        <n-card size="small" :bordered="false" style="background: #fefce8; min-width: 160px;">
          <n-statistic label="好评率" :value="positiveRateDisplay">
            <template #suffix>%</template>
          </n-statistic>
        </n-card>
      </n-space>

      <!-- 反馈列表 -->
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :bordered="true"
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
  </n-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NSpace, NStatistic, NTag, NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const list = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = ref({ total: 0, positive: 0, negative: 0, positiveRate: 0 })

const positiveRateDisplay = computed(() => (stats.value.positiveRate * 100).toFixed(1))

const columns = [
  {
    title: '反馈类型',
    key: 'positive',
    width: 100,
    render(row: any) {
      return row.positive
        ? h(NTag, { type: 'success', size: 'small' }, () => '好评')
        : h(NTag, { type: 'error', size: 'small' }, () => '差评')
    },
  },
  { title: '问题', key: 'query', ellipsis: true, width: 300 },
  {
    title: '相关文档',
    key: 'docIds',
    width: 200,
    render(row: any) {
      if (!row.docIds?.length) return '-'
      return row.docIds.join(', ')
    },
  },
  { title: '会话ID', key: 'sessionId', width: 220 },
  { title: '时间', key: 'timestamp', width: 180 },
]

const fetchStats = async () => {
  try {
    const res = await request.get('/agent/feedback/stats')
    stats.value = res.data
  } catch (err) {
    console.error('获取反馈统计失败', err)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.post('/agent/feedback/page', {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取反馈列表失败', err)
    message.error('获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number) => { currentPage.value = page; fetchList() }
const onPageSizeChange = (size: number) => { pageSize.value = size; currentPage.value = 1; fetchList() }

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
