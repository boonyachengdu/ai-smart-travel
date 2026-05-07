<template>
  <n-card title="企业管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.name" placeholder="企业名称" clearable style="width: 180px" />
        <n-input v-model:value="searchParams.userName" placeholder="管理账号" clearable style="width: 180px" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button v-if="hasSuperAdminPermission" type="info" @click="openAddModal">新增企业</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="companyList"
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
        :title="formData.id ? '编辑企业' : '新增企业'"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '600px' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules">
        <n-form-item label="企业名称" path="name" label-width="100">
          <n-input v-model:value="formData.name" placeholder="请输入企业名称" />
        </n-form-item>
        <n-form-item label="管理账号" path="userName" label-width="100">
          <n-input v-model:value="formData.userName" placeholder="企业管理账号（登录用）" :disabled="!!formData.id" />
        </n-form-item>
        <n-form-item label="密码" path="password" label-width="100">
          <n-input
              v-model:value="formData.password"
              type="password"
              show-password-on="click"
              :placeholder="formData.id ? '留空不修改密码' : '请输入密码'"
          />
        </n-form-item>
        <n-form-item label="描述" path="description" label-width="100">
          <n-input v-model:value="formData.description" type="textarea" :rows="3" placeholder="企业简介" />
        </n-form-item>
        <n-form-item label="自动审批" path="autoApprove" label-width="100">
          <n-switch v-model:value="formData.autoApprove" :checked-value="true" :unchecked-value="false" />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="saveCompany">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h, computed } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NInput, NModal,
  NForm, NFormItem, NSwitch, NPopconfirm, NPagination, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const formRef = ref<any>(null)
const initingCompanyId = ref<number | null>(null)
const hasSuperAdminPermission = ref(false)

// 搜索参数
const searchParams = reactive({
  name: '',
  userName: ''
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const companyList = ref<any[]>([])

// 表格列定义
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '企业名称', key: 'name', width: 200 },
  { title: '管理账号', key: 'userName', width: 180 },
  { title: '描述', key: 'description', ellipsis: true, width: 240 },
  {
    title: '自动审批',
    key: 'autoApprove',
    width: 100,
    render(row: any) {
      return row.autoApprove ? '是' : '否'
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 320,
    align: 'center',
    render(row: any) {
      const buttons = []

      buttons.push(
          h(
              NButton,
              { size: 'small', onClick: () => editCompany(row) },
              { default: () => '编辑' }
          )
      )

      buttons.push(
          h(
              NButton,
              {
                size: 'small',
                type: row.autoApprove ? 'warning' : 'success',
                onClick: () => toggleAutoApprove(row)
              },
              { default: () => row.autoApprove ? '关闭自动审批' : '开启自动审批' }
          )
      )

      buttons.push(
          h(
              NButton,
              {
                size: 'small',
                type: 'info',
                loading: initingCompanyId.value === row.id,
                disabled: initingCompanyId.value !== null,
                onClick: () => initDefaultStandard(row)
              },
              { default: () => '生成差标' }
          )
      )

      buttons.push(
          h(
              NPopconfirm,
              { onPositiveClick: () => deleteCompany(row.id) },
              {
                trigger: () => h(
                    NButton,
                    { size: 'small', type: 'error' },
                    { default: () => '删除' }
                ),
                default: () => `确认删除企业 ${row.name}？`
              }
          )
      )

      return h(
          NSpace,
          { justify: 'center', style: { flexWrap: 'wrap' } },
          { default: () => buttons }
      )
    }
  }
]

// 表单数据
const formData = reactive({
  id: null as number | null,
  name: '',
  userName: '',
  password: '',
  description: '',
  autoApprove: false
})

// 表单规则
const formRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入管理账号', trigger: 'blur' }],
  password: computed(() => formData.id ? [{ required: false }] : [{ required: true, message: '请输入密码', trigger: 'blur' }]),
  description: [{ required: false }]
}

// 检查用户权限
const checkPermission = async () => {
  try {
    const res = await request.get('/users/userInfo')
    const userInfo = res.data
    const roles = userInfo?.roles || ''
    hasSuperAdminPermission.value = roles.includes('ROLE_SUPER_ADMIN')
  } catch (err) {
    console.error('获取用户信息失败', err)
    hasSuperAdminPermission.value = false
  }
}

// 获取企业列表
const fetchCompanies = async () => {
  loading.value = true
  try {
    const res = await request.post('/company/page', {
      page: currentPage.value,
      size: pageSize.value,
      name: searchParams.name,
      userName: searchParams.userName
    })
    companyList.value = res.data?.records || []
    total.value = res.data?.total || 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: companyList.value.length
    })
  } catch (err) {
    console.error('获取企业列表失败', err)
    message.error('获取企业列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchCompanies()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1
  fetchCompanies()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCompanies()
}

// 重置搜索
const resetSearch = () => {
  searchParams.name = ''
  searchParams.userName = ''
  currentPage.value = 1
  fetchCompanies()
}

// 打开新增弹窗
const openAddModal = () => {
  Object.assign(formData, {
    id: null,
    name: '',
    userName: '',
    password: '',
    description: '',
    autoApprove: false
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 编辑企业
const editCompany = (row: any) => {
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    userName: row.userName,
    password: '', // 不回显密码
    description: row.description,
    autoApprove: row.autoApprove
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 保存企业
const saveCompany = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return
    try {
      if (formData.id) {
        await request.put(`/company/${formData.id}`, formData)
        message.success('更新成功')
      } else {
        await request.post('/company', formData)
        message.success('新增成功')
      }
      showModal.value = false
      fetchCompanies()
    } catch (err) {
      console.error('保存失败', err)
      message.error('保存失败')
    }
  })
}

// 切换自动审批状态
const toggleAutoApprove = async (row: any) => {
  try {
    await request.put(`/company/${row.id}/toggle-auto-approve`, !row.autoApprove)
    message.success(row.autoApprove ? '已关闭自动审批' : '已开启自动审批')
    fetchCompanies()
  } catch (err) {
    console.error('操作失败', err)
    message.error('操作失败')
  }
}

// 生成企业默认差标
const initDefaultStandard = async (row: any) => {
  if (!row.id) return

  try {
    initingCompanyId.value = row.id
    await request.post(`/standard/batch-init/${row.id}`)
    message.success('差标标准生成成功')
  } catch (err) {
    console.error('生成差标失败', err)
    message.error('生成差标失败')
  } finally {
    initingCompanyId.value = null
  }
}

// 删除企业
const deleteCompany = async (id: number) => {
  try {
    await request.delete(`/company/${id}`)
    message.success('删除成功')
    fetchCompanies()
  } catch (err) {
    console.error('删除失败', err)
    message.error('删除失败')
  }
}

onMounted(() => {
  checkPermission()
  fetchCompanies()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
