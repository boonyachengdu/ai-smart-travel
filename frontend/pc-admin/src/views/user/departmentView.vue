<template>
  <n-card title="部门管理" :bordered="false">
    <!-- 搜索栏 + 操作按钮 -->
    <n-space vertical size="large">
      <n-space wrap>
        <n-input
            v-model:value="searchParams.name"
            placeholder="部门名称"
            clearable
            style="width: 180px"
        />
        <n-select
            v-model:value="searchParams.companyId"
            placeholder="选择公司"
            :options="companyOptions"
            clearable
            style="width: 180px"
            @update:value="fetchTree"
        />
        <n-button type="primary" @click="fetchTree">查询</n-button>
        <n-button @click="resetSearch">重置</n-button>
        <n-button type="info" @click="openAddModal(null)">新增根部门</n-button>
      </n-space>

      <!-- 树形表格 -->
      <n-tree
          ref="treeRef"
          :data="treeData"
          :default-expand-all="true"
          :block-line="true"
          :show-line="true"
          :render-label="renderLabel"
          :render-prefix="renderPrefix"
          :loading="loading"
          style="background: #fff; border-radius: 8px; overflow: hidden;"
      />
    </n-space>

    <!-- 新增/编辑弹窗 -->
    <n-modal
        v-model:show="showModal"
        :title="formData.id ? '编辑部门' : '新增部门'"
        preset="dialog"
        :mask-closable="false"
        :style="{ width: '600px' }"
    >
      <n-form ref="formRef" :model="formData" :rules="formRules">
        <n-form-item label="上级部门" label-width="100">
          <n-tree-select
              v-model:value="formData.parentId"
              :options="treeOptions"
              placeholder="选择上级部门（留空为根部门）"
              clearable
          />
        </n-form-item>
        <n-form-item label="部门名称" path="name" label-width="100">
          <n-input v-model:value="formData.name" placeholder="请输入部门名称" />
        </n-form-item>
        <n-form-item label="描述" path="description" label-width="100">
          <n-input v-model:value="formData.description" type="textarea" :rows="3" placeholder="部门简介" />
        </n-form-item>
        <n-form-item label="所属公司" path="companyId" label-width="100">
          <n-select
              v-model:value="formData.companyId"
              placeholder="请选择公司"
              :options="companyOptions"
              clearable
          />
        </n-form-item>
      </n-form>

      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="saveDepartment">保存</n-button>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h, computed } from 'vue'
import {
  NCard,
  NTree,
  NButton,
  NSpace,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NSelect,
  NTreeSelect,
  useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const formRef = ref<any>(null)
const treeRef = ref<any>(null)

// 搜索参数
const searchParams = reactive({
  name: '',
  companyId: null as number | null
})

// 公司下拉选项
const companyOptions = ref<any[]>([])

// 部门树数据
const treeData = ref<any[]>([])

// 用于上级部门选择的树形选项
const treeOptions = computed(() => {
  const convert = (nodes: any[]) => nodes.map(node => ({
    label: node.label,
    value: node.key,
    children: node.children ? convert(node.children) : undefined
  }))
  return convert(treeData.value)
})

// 表单数据
const formData = reactive({
  id: null as number | null,
  parentId: null as number | null,
  companyId: null as number | null,
  name: '',
  description: ''
})

// 表单规则
const formRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择所属公司', trigger: 'change' }]
}

// 加载公司列表（优先执行）
const loadCompanies = async () => {
  try {
    const res = await request.get('/company/list')
    companyOptions.value = res.data.map((c: any) => ({
      label: c.name,
      value: c.id
    }))
  } catch (err) {
    message.error('加载公司列表失败')
  }
}

// 获取部门树形数据
const fetchTree = async () => {
  if (companyOptions.value.length === 0) {
    message.warning('公司列表加载中，请稍后...')
    return
  }

  loading.value = true
  try {
    const res = await request.post('/department/tree', {
      params: {
        name: searchParams.name,
        companyId: searchParams.companyId
      }
    })
    console.log('部门原始数据:', res.data)
    treeData.value = buildTree(res.data || [])
    console.log('构建树结构:', treeData.value)
  } catch (err: any) {
    message.error(err.message || '获取部门树失败')
  } finally {
    loading.value = false
  }
}

// 构建树形结构（后端返回平铺列表，前端递归转树）
const buildTree = (list: any[], parentId: number | null = null) => {
  console.log(`构建层级 - 查找 parentId:`, parentId, '数据:', list.map(i => ({ id: i.id, parentId: i.parentId, name: i.name })))

  const result = list
      .filter(item => {
        if (parentId === null) {
          return item.parentId === null || item.parentId === undefined || item.parentId === 0
        }
        return item.parentId === parentId
      })
      .map(item => ({
        key: item.id,
        label: item.name || '未命名部门',
        description: item.description,
        raw: item,
        children: buildTree(list, item.id),
        isLeaf: !list.some(child => child.parentId === item.id)
      }))

  console.log(`层级 ${parentId} 的结果:`, result)
  return result
}

// 渲染节点标签（操作按钮）
const renderLabel = (node: any) => {
  const nodeData = JSON.parse(JSON.stringify(node))
  console.log('渲染节点标签:', nodeData)
  const labelText = nodeData.option.label || '未知部门'
  const description = nodeData.option.description || ''

  return h('div', { style: 'display: flex; align-items: center; justify-content: space-between; width: 100%;' }, [
    h('span', { style: 'flex: 1;' }, [
      labelText,
      description ? h('span', { style: 'color: #999; margin-left: 8px;' }, ` - ${description}`) : null
    ]),
    h('div', { style: 'display: flex; gap: 8px;' }, [
      h(NButton, {
        size: 'tiny',
        type: 'primary',
        quaternary: true,
        onClick: (e: Event) => {
          e.stopPropagation()
          editDepartment(node)
        }
      }, () => '编辑'),
      h(NButton, {
        size: 'tiny',
        type: 'error',
        quaternary: true,
        onClick: (e: Event) => {
          e.stopPropagation()
          deleteDepartment(node)
        }
      }, () => '删除'),
      h(NButton, {
        size: 'tiny',
        type: 'info',
        quaternary: true,
        onClick: (e: Event) => {
          e.stopPropagation()
          openAddModal(node.key)
        }
      }, () => '子部门')
    ])
  ])
}

// 渲染前缀图标
const renderPrefix = () => {
  return h('span', { style: 'margin-right: 4px;' }, '📂')
}

// 重置搜索
const resetSearch = () => {
  searchParams.name = ''
  searchParams.companyId = null
  fetchTree()
}

// 打开新增/编辑弹窗
const openAddModal = (parentId: number | null = null) => {
  Object.assign(formData, {
    id: null,
    parentId,
    companyId: null,
    name: '',
    description: ''
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 编辑部门
const editDepartment = (node: any) => {
  const raw = node.raw
  Object.assign(formData, {
    id: raw.id,
    parentId: raw.parentId,
    companyId: raw.companyId,
    name: raw.name,
    description: raw.description
  })
  formRef.value?.restoreValidation()
  showModal.value = true
}

// 保存部门
const saveDepartment = () => {
  formRef.value?.validate(async (errors: any) => {
    if (errors) return

    try {
      if (formData.id) {
        await request.put(`/department/${formData.id}`, formData)
        message.success('更新成功')
      } else {
        await request.post('/department', formData)
        message.success('新增成功')
      }
      showModal.value = false
      fetchTree()
    } catch (err: any) {
      message.error(err.message || '保存失败')
    }
  })
}

// 删除部门
const deleteDepartment = async (node: any) => {
  const raw = node.raw

  // 检查是否有子部门
  const hasChildren = treeData.value.some((item: any) => item.raw.parentId === raw.id)
  if (hasChildren) {
    message.warning('该部门下有子部门，无法删除')
    return
  }

  try {
    await request.delete(`/department/${raw.id}`)
    message.success('删除成功')
    fetchTree()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

onMounted(async () => {
  // 先加载公司列表
  await loadCompanies()

  // 默认选中第一个公司（可选）
  if (companyOptions.value.length > 0) {
    searchParams.companyId = companyOptions.value[0].value
  }

  // 再加载部门树
  fetchTree()
})
</script>

<style scoped>
.n-tree {
  padding: 16px;
  min-height: 400px;
}
</style>
