<template>
  <div class="approval-create">
    <van-nav-bar title="出差申请" left-arrow @click-left="goBack" />

    <div class="form-container">
      <!-- 出差事由 -->
      <van-field
          v-model="formData.reason"
          name="reason"
          label="出差事由"
          type="textarea"
          rows="3"
          placeholder="请输入出差事由"
          :rules="[{ required: true, message: '请填写出差事由' }]"
      />

      <!-- 出行项列表 -->
      <div class="trip-items-section">
        <div class="section-header">
          <span class="section-title">出行项</span>
          <van-button type="primary" size="small" icon="plus" @click="addTripItem">添加出行项</van-button>
        </div>

        <van-collapse v-model="activeCollapse">
          <van-collapse-item
              v-for="(item, index) in formData.tripItems"
              :key="item.uuid"
              :title="getTripItemTitle(item)"
              :name="index"
          >
            <div class="trip-item-form">
              <!-- 出行类型 -->
              <van-field
                  v-model="item.tripType"
                  label="出行类型"
                  placeholder="请选择"
                  readonly
                  is-link
                  @click="showDatePickerType = 'tripType'"
                  :rules="[{ required: true, message: '请选择出行类型' }]"
              />

              <!-- 出行项名称 -->
              <van-field
                  v-model="item.tripName"
                  label="名称"
                  placeholder="如：北京到上海航班"
                  :rules="[{ required: true, message: '请填写出行项名称' }]"
              />

              <!-- 出发地/入住城市 -->
              <van-field
                  v-model="item.departure"
                  :label="getDepartureLabel(item.tripType)"
                  placeholder="请输入"
                  :rules="[{ required: true, message: '请填写' }]"
              />

              <!-- 目的地/离店城市 -->
              <van-field
                  v-model="item.arrival"
                  :label="getArrivalLabel(item.tripType)"
                  placeholder="请输入"
                  :rules="[{ required: true, message: '请填写' }]"
              />

              <!-- 开始时间 -->
              <van-field
                  v-model="item.startTime"
                  :label="getStartTimeLabel(item.tripType)"
                  placeholder="请选择"
                  readonly
                  is-link
                  @click="openDateTimePicker('startTime')"
                  :rules="[{ required: true, message: '请选择时间' }]"
              />

              <!-- 结束时间 -->
              <van-field
                  v-model="item.endTime"
                  :label="getEndTimeLabel(item.tripType)"
                  placeholder="请选择"
                  readonly
                  is-link
                  @click="openDateTimePicker('endTime')"
                  :rules="[{ required: true, message: '请选择时间' }]"
              />

              <!-- 预估金额 -->
              <van-field
                  v-model="item.estimatedAmount"
                  label="预估金额"
                  type="number"
                  placeholder="请输入"
                  :rules="[{ required: true, message: '请填写预估金额' }]"
              >
                <template #button>
                  <span style="color: #999;">元</span>
                </template>
              </van-field>

              <!-- 删除按钮 -->
              <div class="delete-btn">
                <van-button type="danger" size="small" block plain @click="removeTripItem(index)">
                  删除此项
                </van-button>
              </div>
            </div>
          </van-collapse-item>
        </van-collapse>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-btn">
        <van-button type="primary" block round :loading="submitting" @click="onSubmit">
          {{ submitting ? '提交中...' : '提交申请' }}
        </van-button>
      </div>
    </div>

    <!-- 出行类型选择器 -->
    <van-popup v-model:show="showTripTypePicker" position="bottom">
      <van-picker
          :columns="tripTypeOptions"
          @confirm="onTripTypeConfirm"
          @cancel="showTripTypePicker = false"
      />
    </van-popup>

    <!-- 日期时间选择器 -->
    <van-popup v-model:show="showDatePicker" position="bottom">
      <van-date-picker
          v-model="currentDatePickerValue"
          title="选择日期"
          @confirm="onDateConfirm"
          @cancel="showDatePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import request from '@/utils/request.ts'

const router = useRouter()

interface TripItem {
  uuid: string
  tripType: string
  tripName: string
  departure: string
  arrival: string
  startTime: string
  endTime: string
  estimatedAmount: number
}

interface ApprovalForm {
  reason: string
  tripItems: TripItem[]
}

const formData = reactive<ApprovalForm>({
  reason: '',
  tripItems: []
})

const activeCollapse = ref<number[]>([0])
const submitting = ref(false)

// 选择器相关
const showTripTypePicker = ref(false)
const showDatePicker = ref(false)
const currentDatePickerValue = ref<string[]>([])
const currentEditingItemIndex = ref(-1)
const currentField = ref<'startTime' | 'endTime'>('startTime')

const minDate = new Date()
const maxDate = new Date(new Date().setFullYear(new Date().getFullYear() + 1))

const tripTypeOptions = [
  { text: '机票', value: 'FLIGHT' },
  { text: '酒店', value: 'HOTEL' },
  { text: '火车', value: 'TRAIN' },
  { text: '用车', value: 'CAR' }
]

const goBack = () => {
  router.back()
}

const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

const addTripItem = () => {
  formData.tripItems.push({
    uuid: generateUUID(),
    tripType: '',
    tripName: '',
    departure: '',
    arrival: '',
    startTime: '',
    endTime: '',
    estimatedAmount: 0
  })
  activeCollapse.value = [formData.tripItems.length - 1]
}

const removeTripItem = (index: number) => {
  if (formData.tripItems.length === 1) {
    showToast('至少保留一个出行项')
    return
  }
  formData.tripItems.splice(index, 1)
  if (activeCollapse.value.includes(index)) {
    activeCollapse.value = []
  }
}

const getTripItemTitle = (item: TripItem) => {
  if (!item.tripType) return '新增出行项'
  const typeMap: Record<string, string> = {
    'FLIGHT': '机票',
    'HOTEL': '酒店',
    'TRAIN': '火车',
    'CAR': '用车'
  }
  return `${typeMap[item.tripType] || '出行项'} - ${item.tripName || '未命名'}`
}

const getDepartureLabel = (tripType: string) => {
  const map: Record<string, string> = {
    'FLIGHT': '出发城市',
    'HOTEL': '入住城市',
    'TRAIN': '出发城市',
    'CAR': '上车地点'
  }
  return map[tripType] || '出发地'
}

const getArrivalLabel = (tripType: string) => {
  const map: Record<string, string> = {
    'FLIGHT': '到达城市',
    'HOTEL': '离店城市',
    'TRAIN': '到达城市',
    'CAR': '下车地点'
  }
  return map[tripType] || '目的地'
}

const getStartTimeLabel = (tripType: string) => {
  const map: Record<string, string> = {
    'FLIGHT': '起飞时间',
    'HOTEL': '入住时间',
    'TRAIN': '发车时间',
    'CAR': '用车时间'
  }
  return map[tripType] || '开始时间'
}

const getEndTimeLabel = (tripType: string) => {
  const map: Record<string, string> = {
    'FLIGHT': '到达时间',
    'HOTEL': '离店时间',
    'TRAIN': '到达时间',
    'CAR': '结束时间'
  }
  return map[tripType] || '结束时间'
}

const onTripTypeConfirm = ({ selectedOptions }: any) => {
  if (currentEditingItemIndex.value >= 0 && currentEditingItemIndex.value < formData.tripItems.length) {
    formData.tripItems[currentEditingItemIndex.value].tripType = selectedOptions[0].value
  }
  showTripTypePicker.value = false
}

const openDateTimePicker = (field: 'startTime' | 'endTime') => {
  currentField.value = field
  const item = formData.tripItems[currentEditingItemIndex.value]
  if (!item) return

  const dateStr = field === 'startTime' ? item.startTime : item.endTime
  if (dateStr) {
    // 如果已有值，解析为数组格式 ['2026', '03', '15']
    const date = new Date(dateStr)
    currentDatePickerValue.value = [
      date.getFullYear().toString(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0')
    ]
  } else {
    // 使用当前日期
    const now = new Date()
    currentDatePickerValue.value = [
      now.getFullYear().toString(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0')
    ]
  }
  showDatePicker.value = true
}

const onDateConfirm = (values: string[]) => {
  const [year, month, day] = values
  const hours = '00'
  const minutes = '00'
  const seconds = '00'
  const formatted = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`

  if (currentEditingItemIndex.value >= 0 && currentEditingItemIndex.value < formData.tripItems.length) {
    if (currentField.value === 'startTime') {
      formData.tripItems[currentEditingItemIndex.value].startTime = formatted
    } else {
      formData.tripItems[currentEditingItemIndex.value].endTime = formatted
    }
  }
  showDatePicker.value = false
}

const onSubmit = async () => {
  // 校验表单
  if (!formData.reason.trim()) {
    showToast('请填写出差事由')
    return
  }

  if (formData.tripItems.length === 0) {
    showToast('请至少添加一个出行项')
    return
  }

  // 校验每个出行项
  for (let i = 0; i < formData.tripItems.length; i++) {
    const item = formData.tripItems[i]
    if (!item.tripType) {
      showToast(`第${i + 1}个出行项请选择类型`)
      activeCollapse.value = [i]
      return
    }
    if (!item.tripName.trim()) {
      showToast(`第${i + 1}个出行项请填写名称`)
      activeCollapse.value = [i]
      return
    }
    if (!item.departure.trim()) {
      showToast(`第${i + 1}个出行项请填写${getDepartureLabel(item.tripType)}`)
      activeCollapse.value = [i]
      return
    }
    if (!item.arrival.trim()) {
      showToast(`第${i + 1}个出行项请填写${getArrivalLabel(item.tripType)}`)
      activeCollapse.value = [i]
      return
    }
    if (!item.startTime) {
      showToast(`第${i + 1}个出行项请选择${getStartTimeLabel(item.tripType)}`)
      activeCollapse.value = [i]
      return
    }
    if (!item.endTime) {
      showToast(`第${i + 1}个出行项请选择${getEndTimeLabel(item.tripType)}`)
      activeCollapse.value = [i]
      return
    }
    if (!item.estimatedAmount || item.estimatedAmount <= 0) {
      showToast(`第${i + 1}个出行项请填写预估金额`)
      activeCollapse.value = [i]
      return
    }
  }

  submitting.value = true

  try {
    const payload = {
      reason: formData.reason,
      status: 'PENDING',
      tripItems: formData.tripItems.map(item => ({
        uuid: item.uuid,
        tripType: item.tripType,
        tripName: item.tripName,
        departure: item.departure,
        arrival: item.arrival,
        startTime: item.startTime,
        endTime: item.endTime,
        estimatedAmount: item.estimatedAmount
      }))
    }

    await request.post('/approval', payload)

    showSuccessToast('申请提交成功')
    setTimeout(() => {
      router.back()
    }, 1500)
  } catch (err: any) {
    console.error('提交申请失败', err)
    showToast(err.response?.data?.message || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

// 初始化时添加一个出行项
addTripItem()
</script>

<style scoped>
.approval-create {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 100px;
}

.form-container {
  padding: 16px;
}

.trip-items-section {
  margin: 16px 0;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.trip-item-form {
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
}

.delete-btn {
  margin-top: 16px;
}

.submit-btn {
  margin: 32px 16px;
}

:deep(.van-cell) {
  padding: 12px 16px;
}

:deep(.van-cell__title) {
  font-size: 14px;
}

:deep(.van-field__control) {
  font-size: 14px;
}

:deep(.van-collapse-item__title) {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
}

:deep(.van-picker__toolbar) {
  height: 44px;
}
</style>
