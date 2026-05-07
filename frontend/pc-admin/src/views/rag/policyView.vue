<template>
  <n-card title="差旅政策" :bordered="false">
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
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="success" @click="openUploadModal">上传政策文档</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="policyList"
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

    <!-- 上传政策文档弹窗 -->
    <n-modal
        v-model:show="showUploadModal"
        title="上传政策文档"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '600px' }"
    >
      <n-form ref="uploadFormRef" :model="uploadForm" :rules="uploadFormRules">
        <n-form-item label="政策场景" path="scene" label-width="100">
          <n-select
              v-model:value="uploadForm.scene"
              placeholder="请选择政策场景"
              :options="sceneOptions"
              clearable
          />
        </n-form-item>
        <n-form-item label="政策文档" path="file" label-width="100">
          <div style="display: flex; flex-direction: column; gap: 8px;">
            <n-upload
                accept=".pdf,.doc,.docx,.txt,.md"
                :max-size="1024 * 1024 * 20"
                :default-upload="false"
                @change="handleFileChange"
            >
              <n-button type="success">选择文件</n-button>
            </n-upload>
            <div v-if="uploadForm.fileName" style="color: #18a058; font-size: 14px;">
              ✓ 已选择：{{ uploadForm.fileName }}
            </div>
            <div v-else style="color: #999; font-size: 12px;">
              支持格式：PDF、Word、TXT、MD，最大20MB
            </div>
          </div>
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="closeUploadModal">取消</n-button>
        <n-button type="success" :loading="uploading" @click="submitUpload">确认上传</n-button>
      </template>
    </n-modal>

    <!-- 政策详情弹窗 -->
    <n-modal
        v-model:show="showDetailModal"
        title="政策详情"
        preset="dialog"
        :mask-closable="true"
        :style="{ width: '800px', maxHeight: '80vh', overflow: 'auto' }"
    >
      <n-descriptions :column="1" bordered>
        <n-descriptions-item label="政策场景">
          {{ getSceneLabel(currentPolicy?.scene) }}
        </n-descriptions-item>
        <n-descriptions-item label="政策内容">
          <div style="white-space: pre-wrap; word-break: break-all; max-height: 400px; overflow-y: auto;">
            {{ currentPolicy?.content }}
          </div>
        </n-descriptions-item>
        <n-descriptions-item label="创建时间">
          {{ currentPolicy?.createTime }}
        </n-descriptions-item>
        <n-descriptions-item label="更新时间">
          {{ currentPolicy?.updateTime }}
        </n-descriptions-item>
      </n-descriptions>

      <template #action>
        <n-button @click="showDetailModal = false">关闭</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NSelect, NModal,
  NForm, NFormItem, NUpload, NDescriptions, NDescriptionsItem,
  NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const uploading = ref(false)
const showUploadModal = ref(false)
const showDetailModal = ref(false)
const uploadFormRef = ref<any>(null)
const currentPolicy = ref<any>(null)

// 搜索参数
const searchParams = reactive({
  scene: null as string | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const policyList = ref<any[]>([])

// 场景下拉选项
const sceneOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
  { label: '用车', value: 'CAR' },
  { label: '问答', value: 'QA' }
]

// 上传表单
const uploadForm = reactive({
  scene: null as string | null,
  file: null as File | null,
  fileName: ''
})

// 上传表单规则
const uploadFormRules = {
  scene: { required: true, message: '请选择政策场景', trigger: 'change' },
  file: {
    required: true,
    message: '请选择政策文档',
    trigger: 'change',
    validator: (rule: any, value: any) => {
      return !!uploadForm.file
    }
  }
}

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '企业名称', key: 'companyName', width: 200 },
  { title: '部门名称', key: 'deptName', width: 180 },
  {
    title: '政策场景',
    key: 'scene',
    width: 100,
    render(row: any) {
      return getSceneLabel(row.scene)
    }
  },
  {
    title: '政策内容',
    key: 'content',
    ellipsis: true,
    width: 400,
    render(row: any) {
      const content = row.content || ''
      return content.length > 100 ? content.substring(0, 100) + '...' : content
    }
  },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render(row: any) {
      return row.enabled ? '启用' : '禁用'
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    align: 'center',
    render(row: any) {
      return h(
          NSpace,
          { justify: 'center' },
          {
            default: () => [
              h(
                  NButton,
                  { size: 'small', onClick: () => viewDetail(row) },
                  { default: () => '查看详情' }
              )
            ]
          }
      )
    }
  }
]

// 获取场景标签
const getSceneLabel = (scene: string) => {
  if (!scene) return '-'
  const option = sceneOptions.find(opt => opt.value === scene)
  return option ? option.label : scene
}

// 获取政策列表
const fetchPolicies = async () => {
  loading.value = true
  try {
    const res = await request.post('/policy/page', {
      page: currentPage.value,
      size: pageSize.value,
      sort: 'updateTime',
      order: 'desc',
      scene: searchParams.scene
    })
    policyList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: policyList.value.length
    })
  } catch (err) {
    console.error('获取政策列表失败', err)
    message.error('获取政策列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchPolicies()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchPolicies()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPolicies()
}

// 重置搜索
const resetSearch = () => {
  searchParams.scene = null
  currentPage.value = 1
  fetchPolicies()
}

// 处理文件选择
const handleFileChange = ({ file }: any) => {
  // 验证文件类型
  const fileExt = file.name?.split('.').pop()?.toLowerCase()
  const allowedExts = ['pdf', 'doc', 'docx', 'txt', 'md']

  if (!allowedExts.includes(fileExt || '')) {
    message.error('仅支持 PDF、Word、TXT、MD 文件')
    return false
  }

  // 验证文件大小
  if (file.file && file.file.size > 20 * 1024 * 1024) {
    message.error('文件大小不能超过 20MB')
    return false
  }

  // 保存文件信息
  uploadForm.file = file.file
  uploadForm.fileName = file.name

  // 清除文件字段的验证错误
  if (uploadFormRef.value) {
    uploadFormRef.value.validate([], (errors: any) => {
      if (errors && errors.file) {
        uploadFormRef.value.restoreValidation()
      }
    })
  }

  return false // 阻止自动上传
}

// 打开上传弹窗
const openUploadModal = () => {
  uploadForm.scene = null
  uploadForm.file = null
  uploadForm.fileName = ''
  showUploadModal.value = true
  // 延迟清除验证，确保表单已渲染
  setTimeout(() => {
    uploadFormRef.value?.restoreValidation()
  }, 100)
}

// 关闭上传弹窗
const closeUploadModal = () => {
  showUploadModal.value = false
  uploadForm.scene = null
  uploadForm.file = null
  uploadForm.fileName = ''
}

// 上传文件
const uploadFile = async () => {
  if (!uploadForm.file) {
    message.error('请选择政策文档')
    return false
  }
  if (!uploadForm.scene) {
    message.error('请选择政策场景')
    return false
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('scene', uploadForm.scene)
  formData.append('file', uploadForm.file)

  try {
    await request.post('/policy/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    message.success('上传成功')
    closeUploadModal()
    fetchPolicies()
    return true
  } catch (err) {
    console.error('上传失败', err)
    message.error('上传失败')
    return false
  } finally {
    uploading.value = false
  }
}

// 提交上传
const submitUpload = async () => {
  // 手动验证表单
  if (!uploadForm.scene) {
    message.error('请选择政策场景')
    uploadFormRef.value?.validate()
    return
  }
  if (!uploadForm.file) {
    message.error('请选择政策文档')
    uploadFormRef.value?.validate()
    return
  }

  await uploadFile()
}

// 查看详情
const viewDetail = (row: any) => {
  currentPolicy.value = row
  showDetailModal.value = true
}

onMounted(() => {
  fetchPolicies()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>