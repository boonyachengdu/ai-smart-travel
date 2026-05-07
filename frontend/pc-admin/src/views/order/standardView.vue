<template>
  <n-card title="差标管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.name" placeholder="标准名称" clearable style="width: 220px" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="info" @click="openAddModal">新增差标标准</n-button>
      </n-space>

      <!-- 数据表格 -->
      <n-data-table
          :columns="columns"
          :data="standardList"
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
        :title="formData.id ? '编辑差标标准' : '新增差标标准'"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '900px', maxHeight: '90vh', overflow: 'auto' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <n-grid :cols="2" :x-gap="16" :y-gap="12">
          <n-grid-item>
            <n-form-item label="标准名称" path="name">
              <n-input v-model:value="formData.name" placeholder="请输入标准名称" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="描述" path="description">
              <n-input v-model:value="formData.description" placeholder="简要描述该标准的适用范围" />
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <!-- 金额标准 - 第一行 -->
        <n-grid :cols="3" :x-gap="16" :y-gap="12">
          <n-grid-item>
            <n-form-item label="机票最高价" path="flightMaxPrice">
              <n-input-number
                  v-model:value="formData.flightMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="100"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="酒店最高价" path="hotelMaxPrice">
              <n-input-number
                  v-model:value="formData.hotelMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="50"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="火车最高价" path="trainMaxPrice">
              <n-input-number
                  v-model:value="formData.trainMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="50"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <!-- 金额标准 - 第二行 -->
        <n-grid :cols="3" :x-gap="16" :y-gap="12">
          <n-grid-item>
            <n-form-item label="用车最高价" path="carMaxPrice">
              <n-input-number
                  v-model:value="formData.carMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="50"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="月度总额" path="monthMaxPrice">
              <n-input-number
                  v-model:value="formData.monthMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="1000"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="年度总额" path="yearMaxPrice">
              <n-input-number
                  v-model:value="formData.yearMaxPrice"
                  placeholder="0"
                  :min="0"
                  :step="5000"
                  style="width: 100%"
                  :precision="2"
              >
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-grid-item>
        </n-grid>

        <!-- 标准内容 -->
        <n-form-item label="标准内容" path="content">
          <n-input
              v-model:value="formData.content"
              type="textarea"
              :rows="6"
              placeholder="请输入详细的差标规则说明，包括适用人员、报销范围、特殊说明等"
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="saveStandard">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal,
  NForm, NFormItem, NPopconfirm, NPagination, useMessage,
  NGrid, NGridItem, NInputNumber
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const formRef = ref<any>(null)

// 搜索参数
const searchParams = reactive({
  name: ''
})

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const standardList = ref<any[]>([])

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '企业名称', key: 'companyName', width: 200 },
  { title: '标准名称', key: 'name', width: 180 },
  {
    title: '机票上限',
    key: 'flightMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.flightMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  {
    title: '酒店上限',
    key: 'hotelMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.hotelMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  {
    title: '火车上限',
    key: 'trainMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.trainMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  {
    title: '用车上限',
    key: 'carMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.carMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  {
    title: '月度总额',
    key: 'monthMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.monthMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  {
    title: '年度总额',
    key: 'yearMaxPrice',
    width: 100,
    render(row: any) {
      return `¥${row.yearMaxPrice?.toFixed(2) || '0.00'}`
    }
  },
  { title: '描述', key: 'description', ellipsis: true, width: 200 },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    align: 'center',
    fixed: 'right',
    render(row: any) {
      return h(
          NSpace,
          { justify: 'center', size: 'small' },
          {
            default: () => [
              h(
                  NButton,
                  { size: 'small', type: 'primary', onClick: () => editStandard(row) },
                  { default: () => '编辑' }
              ),
              h(
                  NPopconfirm,
                  { onPositiveClick: () => deleteStandard(row.id) },
                  {
                    trigger: () => h(
                        NButton,
                        { size: 'small', type: 'error' },
                        { default: () => '删除' }
                    ),
                    default: () => `确认删除差标标准 ${row.name}？`
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
  description: '',
  content: '',
  flightMaxPrice: null as number | null,
  hotelMaxPrice: null as number | null,
  trainMaxPrice: null as number | null,
  carMaxPrice: null as number | null,
  monthMaxPrice: null as number | null,
  yearMaxPrice: null as number | null
})

// 表单验证规则
const formRules = {
  name: [{ required: true, message: '请输入标准名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  content: [{ required: true, message: '请输入标准内容', trigger: 'blur' }],
  flightMaxPrice: [{ required: true, message: '请输入机票最高价', trigger: 'blur' }],
  hotelMaxPrice: [{ required: true, message: '请输入酒店最高价', trigger: 'blur' }],
  trainMaxPrice: [{ required: true, message: '请输入火车最高价', trigger: 'blur' }],
  carMaxPrice: [{ required: true, message: '请输入用车最高价', trigger: 'blur' }],
  monthMaxPrice: [{ required: true, message: '请输入月度总额', trigger: 'blur' }],
  yearMaxPrice: [{ required: true, message: '请输入年度总额', trigger: 'blur' }]
}

// 获取差标列表
const fetchStandards = async () => {
  loading.value = true
  try {
    const res = await request.post('/standard/page', {
      page: currentPage.value,
      size: pageSize.value,
      name: searchParams.name
    })
    standardList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    console.error('获取差标列表失败', err)
    message.error('获取差标列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件
const onPageChange = (page: number) => {
  currentPage.value = page
  fetchStandards()
}

const onPageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchStandards()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchStandards()
}

// 重置搜索
const resetSearch = () => {
  searchParams.name = ''
  currentPage.value = 1
  fetchStandards()
}

// 打开新增弹窗
const openAddModal = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    description: '',
    content: '',
    flightMaxPrice: null,
    hotelMaxPrice: null,
    trainMaxPrice: null,
    carMaxPrice: null,
    monthMaxPrice: null,
    yearMaxPrice: null
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 编辑差标
const editStandard = (row: any) => {
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    description: row.description,
    content: row.content,
    flightMaxPrice: row.flightMaxPrice,
    hotelMaxPrice: row.hotelMaxPrice,
    trainMaxPrice: row.trainMaxPrice,
    carMaxPrice: row.carMaxPrice,
    monthMaxPrice: row.monthMaxPrice,
    yearMaxPrice: row.yearMaxPrice
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 保存差标
const saveStandard = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return
    try {
      if (formData.id) {
        await request.put(`/standard/${formData.id}`, formData)
        message.success('更新成功')
      } else {
        await request.post('/standard', formData)
        message.success('新增成功')
      }
      showModal.value = false
      fetchStandards()
    } catch (err) {
      console.error('保存失败', err)
      message.error('保存失败')
    }
  })
}

// 删除差标
const deleteStandard = async (id: number) => {
  try {
    await request.delete(`/standard/${id}`)
    message.success('删除成功')
    fetchStandards()
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

onMounted(() => {
  fetchStandards()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>