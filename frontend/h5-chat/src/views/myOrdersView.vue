<template>
  <div class="my-orders">
    <van-nav-bar title="我的订单" left-arrow @click-left="goBack" />

    <van-tabs v-model:active="activeTab" animated sticky offset-top="46px" @change="onTabChange">
      <van-tab title="机票" name="flight">
        <div class="order-list">
          <van-loading v-if="loading.flight" type="spinner" size="24" />
          <van-empty v-else-if="!loading.flight && flightOrders.length === 0" description="暂无机票订单" />
          <van-list
              v-else
              v-model:loading="loading.flight"
              :finished="finished.flight"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('flight')"
          >
            <div v-for="order in flightOrders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span :class="['order-status', getStatusClass(order.status)]">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-info">
                <div>{{ getFlightRoute(order) }}</div>
                <div class="order-time">{{ formatTime(order.createTime) }}</div>
              </div>
              <div class="order-footer">
                <span class="order-amount">¥{{ order.amount }}</span>
                <van-button size="small" type="primary" @click="viewDetail(order)">查看详情</van-button>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>

      <van-tab title="酒店" name="hotel">
        <div class="order-list">
          <van-loading v-if="loading.hotel" type="spinner" size="24" />
          <van-empty v-else-if="!loading.hotel && hotelOrders.length === 0" description="暂无酒店订单" />
          <van-list
              v-else
              v-model:loading="loading.hotel"
              :finished="finished.hotel"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('hotel')"
          >
            <div v-for="order in hotelOrders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span :class="['order-status', getStatusClass(order.status)]">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-info">
                <div>{{ getHotelName(order) }}</div>
                <div class="order-time">{{ getHotelDates(order) }}</div>
              </div>
              <div class="order-footer">
                <span class="order-amount">¥{{ order.amount }}</span>
                <van-button size="small" type="primary" @click="viewDetail(order)">查看详情</van-button>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>

      <van-tab title="火车" name="train">
        <div class="order-list">
          <van-loading v-if="loading.train" type="spinner" size="24" />
          <van-empty v-else-if="!loading.train && trainOrders.length === 0" description="暂无火车订单" />
          <van-list
              v-else
              v-model:loading="loading.train"
              :finished="finished.train"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('train')"
          >
            <div v-for="order in trainOrders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span :class="['order-status', getStatusClass(order.status)]">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-info">
                <div>{{ getTrainRoute(order) }}</div>
                <div class="order-time">{{ formatTime(order.createTime) }}</div>
              </div>
              <div class="order-footer">
                <span class="order-amount">¥{{ order.amount }}</span>
                <van-button size="small" type="primary" @click="viewDetail(order)">查看详情</van-button>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>

      <van-tab title="用车" name="car">
        <div class="order-list">
          <van-loading v-if="loading.car" type="spinner" size="24" />
          <van-empty v-else-if="!loading.car && carOrders.length === 0" description="暂无用车订单" />
          <van-list
              v-else
              v-model:loading="loading.car"
              :finished="finished.car"
              finished-text="没有更多了"
              :immediate-check="false"
              @load="onLoadMore('car')"
          >
            <div v-for="order in carOrders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span :class="['order-status', getStatusClass(order.status)]">{{ getStatusText(order.status) }}</span>
              </div>
              <div class="order-info">
                <div>{{ getCarRoute(order) }}</div>
                <div class="order-time">{{ formatTime(order.createTime) }}</div>
              </div>
              <div class="order-footer">
                <span class="order-amount">¥{{ order.amount }}</span>
                <van-button size="small" type="primary" @click="viewDetail(order)">查看详情</van-button>
              </div>
            </div>
          </van-list>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '@/utils/request'

interface Order {
  id: number
  orderNo: string
  orderType: string
  status: string
  amount: number
  createTime: string
  journey?: any[]
}

const flightOrders = ref<Order[]>([])
const hotelOrders = ref<Order[]>([])
const trainOrders = ref<Order[]>([])
const carOrders = ref<Order[]>([])

const loading = ref({
  flight: false,
  hotel: false,
  train: false,
  car: false
})

const finished = ref({
  flight: false,
  hotel: false,
  train: false,
  car: false
})

const pagination = ref({
  flight: { page: 1, size: 10 },
  hotel: { page: 1, size: 10 },
  train: { page: 1, size: 10 },
  car: { page: 1, size: 10 }
})

const activeTab = ref('flight')
const router = useRouter()

const goBack = () => {
  router.back()
}

const viewDetail = (order: any) => {
  showToast(`查看订单详情：${order.orderNo}`)
}

const getStatusClass = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': 'pending',
    'PAID': 'completed',
    'COMPLETED': 'completed',
    'CANCELLED': 'cancelled',
    'REFUNDED': 'cancelled'
  }
  return map[status] || 'pending'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'ORDERING': '下单中',
    'PENDING_PAYMENT': '待支付',
    'PAID': '已支付',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'REFUNDED': '已退款'
  }
  return map[status] || status
}

const getFlightRoute = (order: Order) => {
  if (!order.journey || order.journey.length === 0) return '行程信息加载中...'
  const journey = order.journey[0]
  return `${journey.departure || ''} → ${journey.arrival || ''}`
}

const getHotelName = (order: Order) => {
  if (!order.journey || order.journey.length === 0) return '酒店信息加载中...'
  return order.journey[0]?.hotelName || '未知酒店'
}

const getHotelDates = (order: Order) => {
  if (!order.journey || order.journey.length === 0) return ''
  const journey = order.journey[0]
  return `${journey.checkInDate || ''} ~ ${journey.checkOutDate || ''}`
}

const getTrainRoute = (order: Order) => {
  if (!order.journey || order.journey.length === 0) return '行程信息加载中...'
  const journey = order.journey[0]
  return `${journey.departure || ''} → ${journey.arrival || ''}`
}

const getCarRoute = (order: Order) => {
  if (!order.journey || order.journey.length === 0) return '行程信息加载中...'
  const journey = order.journey[0]
  return `${journey.pickupLocation || ''} → ${journey.dropoffLocation || ''}`
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const loadOrders = async (type: string, isLoadMore = false) => {
  const key = type as keyof typeof loading
  const paginationKey = type as keyof typeof pagination

  loading.value[key] = true

  try {
    const currentPage = pagination.value[paginationKey].page
    const pageSize = pagination.value[paginationKey].size

    const res = await request.post('/order/page', {
      page: currentPage,
      size: pageSize,
      orderType: type.toUpperCase()
    })

    const orders = res.data?.records || []
    const total = res.data?.total || 0

    if (isLoadMore) {
      // 加载更多：追加数据
      switch (type) {
        case 'flight':
          flightOrders.value = [...flightOrders.value, ...orders]
          break
        case 'hotel':
          hotelOrders.value = [...hotelOrders.value, ...orders]
          break
        case 'train':
          trainOrders.value = [...trainOrders.value, ...orders]
          break
        case 'car':
          carOrders.value = [...carOrders.value, ...orders]
          break
      }
    } else {
      // 首次加载：替换数据
      switch (type) {
        case 'flight':
          flightOrders.value = orders
          break
        case 'hotel':
          hotelOrders.value = orders
          break
        case 'train':
          trainOrders.value = orders
          break
        case 'car':
          carOrders.value = orders
          break
      }
    }

    // 判断是否已经加载完所有数据
    if (orders.length < pageSize || flightOrders.value.length >= total) {
      finished.value[key] = true
    } else {
      finished.value[key] = false
      pagination.value[paginationKey].page = currentPage + 1
    }
  } catch (err: any) {
    console.error('加载订单失败', err)
    showToast(err.response?.data?.message || '加载订单失败')
    finished.value[key] = true
  } finally {
    loading.value[key] = false
  }
}

const onLoadMore = (type: string) => {
  loadOrders(type, true)
}

const onTabChange = (name: string) => {
  const type = name as string
  const paginationKey = type as keyof typeof pagination

  // 如果该 tab 还没有加载过数据，则首次加载
  if (pagination.value[paginationKey].page === 1) {
    loadOrders(type, false)
  }
}

onMounted(() => {
  loadOrders('flight', false)
})
</script>

<style scoped>
.my-orders {
  min-height: 100vh;
  background: #f8f8f8;
}

.order-list {
  padding: 16px;
  min-height: calc(100vh - 100px);
}

.order-card {
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.order-no {
  color: #333;
  font-weight: bold;
}

.order-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
}

.pending { background: #fff7e6; color: #fa8c16; }
.completed { background: #f0f9eb; color: #52c41a; }
.cancelled { background: #f5f5f5; color: #999; }

.order-info {
  margin-bottom: 12px;
  color: #666;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.order-amount {
  color: #ee0a24;
  font-weight: bold;
}
</style>
