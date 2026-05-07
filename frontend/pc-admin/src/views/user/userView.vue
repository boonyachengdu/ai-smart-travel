<template>
  <n-card title="用户管理" :bordered="false">
    <!-- 搜索栏 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input v-model:value="searchParams.username" placeholder="用户名" clearable style="width: 180px" />
        <n-input v-model:value="searchParams.phone" placeholder="手机号" clearable style="width: 180px" />
        <n-button type="primary" @click="handleSearch">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="info" @click="openAddModal">新增用户</n-button>
      </n-space>

      <!-- 数据表格 - 不使用内置分页 -->
      <n-data-table
          :columns="columns"
          :data="userList"
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
        v-model:show="showAddModal"
        title="新增/编辑用户"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '600px' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules">
        <n-form-item label="用户名" path="username" label-width="80">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码" path="password" label-width="80">
          <n-input
              v-model:value="formData.password"
              type="password"
              show-password-on="click"
              placeholder="新增必填，编辑可选"
          />
        </n-form-item>
        <n-form-item label="手机号" path="phone" label-width="80">
          <n-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </n-form-item>
        <n-form-item label="邮箱" path="email" label-width="80">
          <n-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="角色" path="roles" label-width="80">
          <n-select
              v-model:value="formData.roles"
              :options="roleOptions"
              multiple
              placeholder="请选择角色（可多选）"
              style="width: 100%"
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showAddModal = false">取消</n-button>
        <n-button type="primary" @click="saveUser">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h, computed } from 'vue'
import {
  NCard,
  NDataTable,
  NButton,
  NSpace,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NPopconfirm,
  NSelect,
  NTag,
  NPagination,
  useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showAddModal = ref(false)
const formRef = ref<any>(null)

const searchParams = reactive({
  username: '',
  phone: ''
})

// 分页相关 - 使用独立的 ref
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userList = ref<any[]>([])

// 角色选项
const roleOptions = [
  { label: '超级管理员', value: 'ROLE_SUPER_ADMIN' },
  { label: '管理员', value: 'ROLE_ADMIN' },
  { label: '审核员', value: 'ROLE_AUDITOR' },
  { label: '普通用户', value: 'ROLE_USER' },
  { label: '游客', value: 'ROLE_GUEST' }
]

// 表格列配置
const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username', width: 150 },
  { title: '企业名称', key: 'companyName', width: 200 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '邮箱', key: 'email', width: 200 },
  {
    title: '权限角色',
    key: 'roles',
    width: 250,
    render(row: any) {
      let roles: any[] = []
      if (row.roles) {
        if (Array.isArray(row.roles)) {
          roles = row.roles
        } else if (typeof row.roles === 'string') {
          try {
            const parsed = JSON.parse(row.roles)
            roles = Array.isArray(parsed) ? parsed : [parsed]
          } catch (e) {
            roles = row.roles.split(',').map((r: string) => r.trim())
          }
        } else {
          roles = [row.roles]
        }
      }

      return h(
          NSpace,
          { wrap: true },
          {
            default: () => roles.map((role: any) => {
              const roleName = String(role)
              const roleMap: Record<string, string> = {
                'ROLE_SUPER_ADMIN': '超级管理员',
                'ROLE_ADMIN': '管理员',
                'ROLE_AUDITOR': '审核员',
                'ROLE_USER': '普通用户',
                'ROLE_GUEST': '游客'
              }
              const colorMap: Record<string, string> = {
                'ROLE_SUPER_ADMIN': 'error',
                'ROLE_ADMIN': 'warning',
                'ROLE_AUDITOR': 'info',
                'ROLE_USER': 'success',
                'ROLE_GUEST': 'default'
              }
              return h(
                  NTag,
                  {
                    type: colorMap[roleName] || 'default',
                    size: 'small',
                    style: { marginRight: '4px' }
                  },
                  { default: () => roleMap[roleName] || roleName }
              )
            })
          }
      )
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    align: 'center',
    render(row: any) {
      return h(
          NSpace,
          { justify: 'center' },
          {
            default: () => [
              h(
                  NButton,
                  {
                    size: 'small',
                    onClick: () => editUser(row)
                  },
                  { default: () => '编辑' }
              ),
              h(
                  NPopconfirm,
                  {
                    onPositiveClick: () => deleteUser(row.id)
                  },
                  {
                    trigger: () =>
                        h(
                            NButton,
                            { size: 'small', type: 'error' },
                            { default: () => '删除' }
                        ),
                    default: () => `确认删除用户 ${row.username}？`
                  }
              )
            ]
          }
      )
    }
  }
]

const formData = reactive({
  id: null as number | null,
  username: '',
  password: '',
  phone: '',
  email: '',
  roles: [] as string[]
})

// 动态表单规则
const formRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: formData.id
      ? [{ required: false }]
      : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [{ required: false }],
  email: [{ required: false }],
  roles: [{ required: true, message: '请至少选择一个角色', trigger: 'change', type: 'array' }]
}))

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.post('/users/page', {
      page: currentPage.value,
      size: pageSize.value,
      sort: 'updateTime',
      order: 'desc',
      ...searchParams
    })

    userList.value = res.data?.records || []
    total.value = res.data?.total ?? 0

    console.log('分页数据:', {
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      total: total.value,
      recordsCount: userList.value.length
    })
  } catch (err: any) {
    console.error('获取用户列表失败', err)
    message.error(err.response?.data?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const onPageChange = (page: number) => {
  console.log('切换到第', page, '页')
  currentPage.value = page
  fetchUsers()
}

const onPageSizeChange = (size: number) => {
  console.log('每页显示', size, '条')
  pageSize.value = size
  currentPage.value = 1  // 重置到第一页
  fetchUsers()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1  // 搜索时重置到第一页
  fetchUsers()
}

// 重置搜索
const resetSearch = () => {
  searchParams.username = ''
  searchParams.phone = ''
  currentPage.value = 1
  fetchUsers()
}

// 打开新增弹窗
const openAddModal = () => {
  Object.assign(formData, {
    id: null,
    username: '',
    password: '',
    phone: '',
    email: '',
    roles: []
  })
  formRef.value?.restoreValidation()
  showAddModal.value = true
}

// 编辑用户
const editUser = (row: any) => {
  let rolesArray: string[] = []
  if (row.roles) {
    if (typeof row.roles === 'string') {
      rolesArray = row.roles.split(',').map((r: string) => r.trim())
    } else if (Array.isArray(row.roles)) {
      rolesArray = row.roles
    }
  }

  Object.assign(formData, {
    id: row.id,
    username: row.username,
    password: '',
    phone: row.phone,
    email: row.email,
    roles: rolesArray
  })
  formRef.value?.restoreValidation()
  showAddModal.value = true
}

// 保存用户
const saveUser = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return

    try {
      const payload = {
        ...formData,
        roles: formData.roles.join(',')
      }

      if (formData.id) {
        await request.put(`/users/${formData.id}`, payload)
        message.success('更新成功')
      } else {
        await request.post('/users', payload)
        message.success('新增成功')
      }
      showAddModal.value = false
      fetchUsers()
    } catch (err: any) {
      console.error('保存失败', err)
      message.error(err.response?.data?.message || '保存失败')
    }
  })
}

// 删除用户
const deleteUser = async (id: number) => {
  try {
    await request.delete(`/users/${id}`)
    message.success('删除成功')
    fetchUsers()
  } catch (err: any) {
    console.error('删除失败', err)
    message.error(err.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>