<template>
  <n-card title="文件记录" :bordered="false">
    <!-- 操作栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.fileName" placeholder="文件名" clearable style="width: 200px" />
        <n-select
            v-model:value="searchParams.scene"
            placeholder="场景"
            :options="sceneOptions"
            clearable
            style="width: 150px"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
      </n-space>

      <!-- 文件列表 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="fileList"
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
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NSelect,
  NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)

// 搜索参数
const searchParams = reactive({
  fileName: '',
  scene: null as string | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fileList = ref<any[]>([])

// 场景选项
const sceneOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
  { label: '用车', value: 'CAR' },
  { label: '问答', value: 'QA' }
]

// 表格列
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '企业名称', key: 'companyName', width: 200 },
  {
    title: '场景',
    key: 'scene',
    width: 100,
    render(row: any) {
      const option = sceneOptions.find(opt => opt.value === row.scene)
      return option ? option.label : row.scene || '-'
    }
  },
  { title: '文件名', key: 'fileName', width: 240 },
  {
    title: '大小',
    key: 'fileSize',
    width: 120,
    render(row: any) {
      const size = row.fileSize || 0
      return size < 1024 * 1024 ? `${(size / 1024).toFixed(2)} KB` : `${(size / (1024 * 1024)).toFixed(2)} MB`
    }
  },
  { title: '类型', key: 'mimeType', width: 160 },
  { title: '上传人', key: 'username', width: 120 },
  { title: '描述', key: 'description', ellipsis: true, width: 200 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    align: 'center',
    render(row: any) {
      return h(
          NButton,
          { size: 'small', type: 'primary', onClick: () => downloadFile(row) },
          { default: () => '下载' }
      )
    }
  }
]

// 获取文件列表
const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await request.post('/rag-file/page', {
      page: currentPage.value,
      size: pageSize.value,
      sort: 'createTime',
      order: 'desc',
      fileName: searchParams.fileName,
      scene: searchParams.scene
    })
    fileList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: fileList.value.length
    })
  } catch (err) {
    console.error('获取文件列表失败', err)
    message.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchFiles()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchFiles()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFiles()
}

// 重置搜索
const resetSearch = () => {
  searchParams.fileName = ''
  searchParams.scene = null
  currentPage.value = 1
  fetchFiles()
}

// 上传前校验
const beforeUpload = ({ file }: any) => {
  const allowTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown'
  ]
  if (!allowTypes.includes(file.type)) {
    message.error('仅支持 PDF、Word、TXT、MD 文件')
    return false
  }
  if (file.size > 20 * 1024 * 1024) {
    message.error('文件大小不能超过 20MB')
    return false
  }
  return true
}

// 自定义上传（调用后端接口）
const customRequest = async ({ file }: any) => {
  const formData = new FormData()
  formData.append('file', file.file)

  try {
    const res = await request.post('/rag-file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    message.success('上传成功')
    fetchFiles()
  } catch (err) {
    console.error('上传失败', err)
    message.error('上传失败')
  }
}

// 下载文件
const downloadFile = (row: any) => {
  message.info(`下载文件：${row.fileName}`)
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
