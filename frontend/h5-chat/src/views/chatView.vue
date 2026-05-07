<template>
  <div class="chat-page">
    <van-nav-bar :title="sceneTitle" left-arrow @click-left="goBack" />

    <div class="message-list" ref="messageList">
      <ChatBubble
          v-for="msg in chatStore.messages"
          :key="msg.id"
          :role="msg.role"
          :content="msg.content"
          :solutions="msg.solutions"
          :selected-solution-index="msg.selectedSolutionIndex"
          @select-solution="handleSelectSolution"
      />

      <div v-if="chatStore.messages.length === 0" class="empty-tip">
        {{ sceneGreeting }}
      </div>

      <div v-if="loading" class="loading-tip">
        <van-loading type="spinner" size="24" /> 思考中...
      </div>
    </div>

    <MessageInput @send="sendMessage" @upload-success="handleUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'
import request from '@/utils/request'
import ChatBubble from '@/components/chatBubble.vue'
import MessageInput from '@/components/messageInput.vue'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const messageList = ref<HTMLDivElement | null>(null)
const loading = ref(false)
const sessionId = ref<number | null>(null)
const isFirstSend = ref(true)

const scene = (route.params.scene as string) || 'default'
console.log('场景：', scene)

const autoSendMessages: Record<string, string> = {
  flight: '订机票',
  hotel: '订酒店',
  train: '订火车票',
  car: '用车/打车'
}

const sceneInfo = computed(() => {
  const map: Record<string, { title: string; greeting: string }> = {
    flight: { title: '机票预订助手', greeting: '您好！请告诉我您的出发地、目的地、日期和舱位偏好～' },
    hotel: { title: '酒店预订助手', greeting: '欢迎使用酒店预订！需要哪个城市、入住/离店日期、星级或品牌吗？' },
    train: { title: '火车票助手', greeting: '火车票查询启动！请提供出发地、目的地、日期和车次偏好～' },
    car: { title: '用车服务助手', greeting: '用车需求？起点、终点、用车时间、人数告诉我吧～' },
    default: { title: '智能差旅助手', greeting: '欢迎使用智能差旅助手！有什么出行问题都可以问我～' },
  }
  return map[scene] || map.default
})

const sceneTitle = computed(() => sceneInfo.value!.title)
const sceneGreeting = computed(() => sceneInfo.value!.greeting)

watch(() => route.params.scene, (newScene, oldScene) => {
  if (newScene && newScene !== oldScene) {
    console.log('场景切换：', oldScene, '->', newScene)
    chatStore.clearMessages()
    sessionId.value = null
    isFirstSend.value = true

    if (oldScene) {
      localStorage.removeItem(`session_${oldScene}`)
    }

    const savedSid = localStorage.getItem(`session_${newScene}`)
    if (!savedSid) {
      console.log('新场景无历史记录，将创建新会话')
    } else {
      console.log('新场景有历史记录，将加载')
      sessionId.value = parseInt(savedSid)
      loadHistory(sessionId.value)
      isFirstSend.value = false
    }
  }
}, { immediate: false })

onMounted(() => {
  if (chatStore.messages.length === 0) {
    chatStore.addMessage({
      role: 'assistant',
      content: sceneGreeting.value,
    })
  }
  scrollToBottom()

  const savedSid = localStorage.getItem(`session_${scene}`)
  if (savedSid) {
    sessionId.value = parseInt(savedSid)
    loadHistory(sessionId.value)
    isFirstSend.value = false
  } else {
    const autoMessage = autoSendMessages[scene]
    if (autoMessage && scene !== 'default') {
      nextTick(() => {
        sendMessage(autoMessage)
        isFirstSend.value = false
      })
    }
  }
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

const sendMessage = async (content: string, selectedSolutionIndex?: number) => {
  if (!content.trim()) return

  const userMsg = { role: 'user' as const, content }
  chatStore.addMessage(userMsg)
  scrollToBottom()

  loading.value = true

  try {
    const payload: any = {
      scene: scene.toUpperCase(),
      userInput: content,
      newSession: isFirstSend.value
    }

    if (sessionId.value !== null) {
      payload.sessionId = sessionId.value
      payload.newSession = false
    }

    if (selectedSolutionIndex !== undefined) {
      payload.selectedSolution = selectedSolutionIndex
    }

    const res = await request.post('/dialog/chat/session', payload)

    const responseData = res.data

    sessionId.value = responseData.response?.sessionId

    if (responseData.response?.sessionId && !localStorage.getItem(`session_${scene}`)) {
      localStorage.setItem(`session_${scene}`, String(responseData.response.sessionId))
    }

    const response = responseData.response

    let replyContent = '暂无回复'
    let solutions = undefined
    let selectedSolutionIndexResponse = undefined

    if (response) {
      if (response.status === 'clarify' && response.clarifyQuestions?.length > 0) {
        replyContent = '请补充以下信息：\n' + response.clarifyQuestions.map((q: string) => `- ${q}`).join('\n')
      } else if (response.status === 'complete' && response.order) {
        replyContent = `订单已生成！\n订单号：${response.order.orderNo || '生成中'}\n预算金额：¥${response.order.budget || '待确认'}\n状态：${response.order.status || '待支付'}`
      } else if (response.status === 'violation') {
        replyContent = `订单违反差标规则：${response.violationReason || '具体原因未知'}`
      } else if (response.status === 'recommend' && response.solutions?.length > 0) {
        replyContent = response.message || '为您推荐以下方案：'

        solutions = response.solutions.map((sol: any, idx: number) => ({
          ...sol,
          index: sol.index || idx + 1,
          summary: sol.solutionName || `${sol.flightNo} - ${sol.airline} - ${sol.cabinClass}`,
          features: sol.features || `${sol.departureTime ? sol.departureTime.substring(11, 16) : ''}起飞 | ${sol.arrivalTime ? sol.arrivalTime.substring(11, 16) : ''}到达 | ${sol.cabinClass} | ${sol.channel || '未知渠道'}`,
          price: sol.price || 0
        }))

        console.log('处理后的解决方案:', solutions)
      } else {
        replyContent = response.message || JSON.stringify(response)
      }
      console.log('最终回复内容:', replyContent)
    } else {
      console.warn('⚠️ response 为空!')
    }

    chatStore.addMessage({
      role: 'assistant',
      content: replyContent,
      solutions: solutions,
      selectedSolutionIndex: selectedSolutionIndexResponse
    })

    isFirstSend.value = false
  } catch (err: any) {
    console.error('对话失败', err)
    let errMsg = '抱歉，服务异常，请稍后再试'
    if (err.response?.data?.message) {
      errMsg = err.response.data.message
    }
    chatStore.addMessage({
      role: 'assistant',
      content: errMsg
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleSelectSolution = async (index: number) => {
  const lastMessage = chatStore.messages[chatStore.messages.length - 1]
  if (!lastMessage || !lastMessage.solutions) return

  chatStore.updateMessage(lastMessage.id, {
    selectedSolutionIndex: index
  })

  const selectedSolution = lastMessage.solutions.find(s => s.index === index)
  if (!selectedSolution) return

  const confirmMsg = `我选择方案 ${index}：${selectedSolution.summary}（¥${selectedSolution.price}）`

  await sendMessage(confirmMsg, index)
}

const handleUploadSuccess = (filename: string) => {
  chatStore.addMessage({
    role: 'assistant',
    content: `文件 "${filename}" 已上传成功！请告诉我您想问关于这个文件的问题～`
  })
  scrollToBottom()
}

const loadHistory = async (sid: number) => {
  try {
    const res = await request.get(`/dialog/history/${sid}`)
    const history = res.data || []
    chatStore.messages = history.map((msg: any) => ({
      id: msg.id || Date.now() + Math.random(),
      role: msg.role,
      content: msg.content,
      timestamp: new Date(msg.timestamp || Date.now()),
      solutions: msg.solutions,
      selectedSolutionIndex: msg.selectedSolutionIndex
    }))
    scrollToBottom()
  } catch (err) {
    console.error('加载历史失败', err)
  }
}

const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.chat-page { height: 100vh; display: flex; flex-direction: column; }
.message-list { flex: 1; overflow-y: auto; padding: 16px; background: #f5f5f5; }
.loading-tip { text-align: center; padding: 16px; color: #666; }
.empty-tip { text-align: center; color: #666; margin-top: 100px; font-size: 16px; }
</style>
