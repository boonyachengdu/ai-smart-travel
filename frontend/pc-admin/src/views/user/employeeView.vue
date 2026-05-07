<template>
  <n-card title="员工管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.name" placeholder="姓名" clearable style="width: 180px" />
        <n-input v-model:value="searchParams.employeeNo" placeholder="工号" clearable style="width: 180px" />
        <n-input v-model:value="searchParams.position" placeholder="职位" clearable style="width: 180px" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="info" @click="openAddModal">新增员工</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="employeeList"
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
        :title="formData.id ? '编辑员工' : '新增员工'"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '700px' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules">
        <n-form-item label="所属公司" path="companyId" label-width="100">
          <n-select
              v-model:value="formData.companyId"
              placeholder="请选择公司"
              :options="companyOptions"
              clearable
              @update:value="onCompanyChange"
          />
        </n-form-item>
        <n-form-item label="所属部门" path="departmentId" label-width="100">
          <n-select
              v-model:value="formData.departmentId"
              placeholder="请选择部门"
              :options="departmentOptions"
              clearable
          />
        </n-form-item>
        <n-form-item label="关联用户 ID" path="userId" label-width="100">
          <n-input-number v-model:value="formData.userId" placeholder="请输入用户 ID" />
        </n-form-item>
        <n-form-item label="姓名" path="name" label-width="100">
          <n-input v-model:value="formData.name" placeholder="请输入姓名" />
        </n-form-item>
        <n-form-item label="职位" path="position" label-width="100">
          <n-input v-model:value="formData.position" placeholder="请输入职位" />
        </n-form-item>
        <n-form-item label="工号" path="employeeNo" label-width="100">
          <n-input v-model:value="formData.employeeNo" placeholder="请输入工号" />
        </n-form-item>
        <n-form-item label="入职日期" path="entryDate" label-width="100">
          <n-date-picker v-model:value="formData.entryDate" placeholder="请选择入职日期" />
        </n-form-item>
        <n-form-item label="是否默认身份" path="isPrimary" label-width="100">
          <n-switch v-model:value="formData.isPrimary" :checked-value="true" :unchecked-value="false" />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="saveEmployee">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal, NForm,
  NFormItem, NSelect, NSwitch, NDatePicker, NInputNumber,
  NPagination, useMessage, NPopconfirm
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const formRef = ref<any>(null)

// 搜索参数
const searchParams = reactive({
  name: '',
  employeeNo: '',
  position: '',
  companyId: null as number | null,
  departmentId: null as number | null
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const employeeList = ref<any[]>([])

// 公司下拉选项
const companyOptions = ref<any[]>([])

// 部门下拉选项（根据公司动态加载）
const departmentOptions = ref<any[]>([])

// 表格列定义
const columns = [
  {title: 'ID', key: 'id', width: 80},
  {title: '姓名', key: 'name', width: 140},
  {title: '工号', key: 'employeeNo', width: 140},
  {title: '职位', key: 'position', width: 120},
  {title: '所属公司', key: 'companyName', width: 200},
  {title: '所属部门', key: 'departmentName', width: 180},
  {title: '用户名', key: 'username', width: 120},
  {title: '入职日期', key: 'entryDate', width: 140},
  {
    title: '是否默认',
    key: 'isPrimary',
    width: 100,
    render(row: any) {
      return row.isPrimary ? '是' : '否'
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
                  { size: 'small', onClick: () => editEmployee(row) },
                  { default: () => '编辑' }
              ),
              h(
                  NPopconfirm,
                  { onPositiveClick: () => deleteEmployee(row.id) },
                  {
                    trigger: () => h(
                        NButton,
                        { size: 'small', type: 'error' },
                        { default: () => '删除' }
                    ),
                    default: () => `确认删除员工 ${row.employeeNo}？`
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
  companyId: null as number | null,
  departmentId: null as number | null,
  userId: null as number | null,
  position: '',
  name: '',
  employeeNo: '',
  entryDate: null as string | null,
  isPrimary: true
})

// 表单规则
const formRules = {
  companyId: [{ required: true, message: '请选择公司', trigger: 'blur' }],
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  position: [{ required: false }]
}

// 获取员工列表
const fetchEmployees = async () => {
  loading.value = true
  try {
    const res = await request.post('/employee/page', {
      page: currentPage.value,
      size: pageSize.value,
      name: searchParams.name,
      employeeNo: searchParams.employeeNo,
      position: searchParams.position,
      companyId: searchParams.companyId,
      departmentId: searchParams.departmentId
    })
    employeeList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: employeeList.value.length
    })
  } catch (err) {
    console.error('获取员工列表失败', err)
    message.error('获取员工列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchEmployees()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchEmployees()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchEmployees()
}

// 重置搜索
const resetSearch = () => {
  searchParams.name = ''
  searchParams.employeeNo = ''
  searchParams.position = ''
  searchParams.companyId = null
  searchParams.departmentId = null
  currentPage.value = 1
  fetchEmployees()
}

// 公司变更时动态加载部门
const onCompanyChange = async (companyId: number) => {
  if (!companyId) {
    departmentOptions.value = []
    return
  }
  try {
    const res = await request.get('/department/tree', { params: { companyId } })
    departmentOptions.value = res.data.map((d: any) => ({ label: d.name, value: d.id }))
  } catch (err) {
    console.error('加载部门失败', err)
    message.error('加载部门失败')
  }
}

// 打开新增弹窗
const openAddModal = (parentId: number | null = null) => {
  Object.assign(formData, {
    id: null,
    companyId: null,
    departmentId: null,
    userId: null,
    position: '',
    name: '',
    employeeNo: '',
    entryDate: null,
    isPrimary: true
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 编辑员工
const editEmployee = (row: any) => {
  Object.assign(formData, {
    id: row.id,
    companyId: row.companyId,
    departmentId: row.departmentId,
    userId: row.userId,
    position: row.position,
    name: row.name,
    employeeNo: row.employeeNo,
    entryDate: row.entryDate,
    isPrimary: row.isPrimary
  })
  formRef.value?.restoreValidation()
  showModal.value = true
  // 加载部门选项
  onCompanyChange(row.companyId)
}

// 保存员工
const saveEmployee = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return
    try {
      if (formData.id) {
        await request.put(`/employee/${formData.id}`, formData)
        message.success('更新成功')
      } else {
        await request.post('/employee', formData)
        message.success('新增成功')
      }
      showModal.value = false
      fetchEmployees()
    } catch (err) {
      console.error('保存失败', err)
      message.error('保存失败')
    }
  })
}

// 删除员工
const deleteEmployee = async (id: number) => {
  try {
    await request.delete(`/employee/${id}`)
    message.success('删除成功')
    fetchEmployees()
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

onMounted(() => {
  fetchEmployees()
  // 加载公司列表用于下拉
  request.get('/company/list').then(res => {
    companyOptions.value = res.data.map((c: any) => ({ label: c.name, value: c.id }))
  }).catch(err => {
    console.error('加载公司列表失败', err)
  })
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>