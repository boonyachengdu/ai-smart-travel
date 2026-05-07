<template>
  <div class="home-page">
    <!-- 内容区域 -->
    <div class="scene-content">
      <van-nav-bar title="智能差旅助手" />

      <div class="scene-list">
        <van-grid :column-num="2" clickable>
          <van-grid-item
              v-for="scene in scenes"
              :key="scene.name"
              :text="scene.title"
              @click="goToChat(scene.name)"
          >
            <template #icon>
              <img :src="scene.bg" class="scene-bg" />
            </template>
          </van-grid-item>
        </van-grid>
      </div>
    </div>

    <!-- 底部 Tabbar -->
    <van-tabbar v-model="activeTab" route>
      <van-tabbar-item icon="home-o" name="home" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="user-o" name="my" to="/my">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import flightBg from '@/assets/flight_bg.jpg'
import hotelBg from '@/assets/hotel_bg.jpg'
import trainBg from '@/assets/train_bg.jpg'
import carBg from '@/assets/car_bg.jpg'

const router = useRouter()
const activeTab = ref('home')

const scenes = [
  {
    name: 'flight',
    title: '机票预订',
    bg: flightBg,  // 机场候机大厅（真实）
  },
  {
    name: 'hotel',
    title: '酒店预订',
    bg: hotelBg,  // 豪华酒店房间（真实）
  },
  {
    name: 'train',
    title: '火车票预订',
    bg: trainBg,  // 高铁车厢内部（真实）
  },
  {
    name: 'car',
    title: '用车服务',
    bg: carBg,  // 商务轿车行驶中（真实）
  },
]

const goToChat = (scene: string) => {
  router.push(`/chat/${scene}`)
}
</script>

<style scoped>
.home-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.scene-content {
  flex: 1;
  overflow-y: auto;
  background: #f8f8f8;
}

.scene-list {
  padding: 16px;
}

.scene-bg {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
</style>