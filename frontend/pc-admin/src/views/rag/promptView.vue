<template>
  <n-card title="提示词管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.name" placeholder="提示词名称" clearable style="width: 180px" />
        <n-select
            v-model:value="searchParams.enabled"
            placeholder="启用状态"
            :options="enabledOptions"
            clearable
            style="width: 180px"
        />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="info" @click="openAddModal">新增提示词</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="promptList"
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

    <!-- 新增/编辑弹窗 -->
    <n-modal
        v-model:show="showModal"
        :title="formData.id ? '编辑提示词' : '新增提示词'"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '800px' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules">
        <n-form-item label="名称" path="name" label-width="100">
          <n-input v-model:value="formData.name" placeholder="请输入提示词名称" />
        </n-form-item>
        <n-form-item label="提示词内容" path="prompt" label-width="100">
          <n-input
              v-model:value="formData.prompt"
              type="textarea"
              :rows="8"
              placeholder="请输入完整的提示词模板"
          />
        </n-form-item>
        <n-form-item label="描述" path="description" label-width="100">
          <n-input v-model:value="formData.description" type="textarea" :rows="3" placeholder="简要描述提示词用途" />
        </n-form-item>
        <n-form-item label="启用状态" path="enabled" label-width="100">
          <n-switch v-model:value="formData.enabled" :checked-value="true" :unchecked-value="false" />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="savePrompt">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal,
  NForm, NFormItem, NSwitch, NSelect, NPopconfirm,
  NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const formRef = ref<any>(null)

// 搜索参数
const searchParams = reactive({
  name: '',
  enabled: null as boolean | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const promptList = ref<any[]>([])

// 启用状态下拉选项
const enabledOptions = [
  { label: '启用', value: true },
  { label: '禁用', value: false }
]

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '企业名称', key: 'companyName', width: 200 },
  { title: '商旅顾问', key: 'advisor', width: 180 },
  { title: '提示词名称', key: 'name', width: 180 },
  { title: '提示词内容', key: 'prompt', ellipsis: true, width: 300 },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render(row: any) {
      return row.enabled ? '启用' : '禁用'
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    align: 'center',
    render(row: any) {
      return h(
          NSpace,
          { justify: 'center' },
          {
            default: () => [
              h(
                  NButton,
                  { size: 'small', onClick: () => editPrompt(row) },
                  { default: () => '编辑' }
              ),
              h(
                  NButton,
                  {
                    size: 'small',
                    type: row.enabled ? 'warning' : 'success',
                    onClick: () => toggleEnabled(row)
                  },
                  { default: () => row.enabled ? '禁用' : '启用' }
              ),
              h(
                  NPopconfirm,
                  { onPositiveClick: () => deletePrompt(row.id) },
                  {
                    trigger: () => h(
                        NButton,
                        { size: 'small', type: 'error' },
                        { default: () => '删除' }
                    ),
                    default: () => `确认删除提示词 ${row.name}？`
                  }
              )
            ]
          }
      )
    }
  }
]

// 表单数据
const formData = reactive({
  id: null as number | null,
  name: '',
  prompt: '',
  description: '',
  enabled: true
})

// 表单规则
const formRules = {
  name: [{ required: true, message: '请输入提示词名称', trigger: 'blur' }],
  prompt: [{ required: true, message: '请输入提示词内容', trigger: 'blur' }]
}

// 获取提示词列表
const fetchPrompts = async () => {
  loading.value = true
  try {
    const res = await request.post('/prompts/page', {
      page: currentPage.value,
      size: pageSize.value,
      name: searchParams.name,
      enabled: searchParams.enabled
    })
    promptList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: promptList.value.length
    })
  } catch (err) {
    console.error('获取提示词列表失败', err)
    message.error('获取提示词列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchPrompts()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchPrompts()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPrompts()
}

// 重置搜索
const resetSearch = () => {
  searchParams.name = ''
  searchParams.enabled = null
  currentPage.value = 1
  fetchPrompts()
}

// 打开新增弹窗
const openAddModal = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    prompt: '',
    description: '',
    enabled: true
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 编辑提示词
const editPrompt = (row: any) => {
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    prompt: row.prompt,
    description: row.description,
    enabled: row.enabled
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 保存提示词
const savePrompt = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return
    try {
      if (formData.id) {
        await request.put(`/prompts/${formData.id}`, formData)
        message.success('更新成功')
      } else {
        await request.post('/prompts', formData)
        message.success('新增成功')
      }
      showModal.value = false
      fetchPrompts()
    } catch (err) {
      console.error('保存失败', err)
      message.error('保存失败')
    }
  })
}

// 切换启用状态
const toggleEnabled = async (row: any) => {
  try {
    await request.put(`/prompts/${row.id}/toggle-enabled`, !row.enabled)
    message.success(row.enabled ? '已禁用' : '已启用')
    fetchPrompts()
  } catch (err) {
    console.error('操作失败', err)
    message.error('操作失败')
  }
}

// 删除提示词
const deletePrompt = async (id: number) => {
  try {
    await request.delete(`/prompts/${id}`)
    message.success('删除成功')
    fetchPrompts()
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

onMounted(() => {
  fetchPrompts()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>