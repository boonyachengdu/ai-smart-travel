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
        :steps="msg.steps"
        :show-thinking="msg.showThinking"
        :is-streaming="msg.isStreaming"
        :feedback="msg.feedback"
        :message-id="msg.id"
        @select-solution="handleSelectSolution"
        @toggle-thinking="handleToggleThinking(msg.id)"
        @feedback="(type: 'positive' | 'negative') => handleFeedback(msg.id, type)"
      />

      <div v-if="chatStore.messages.length === 0" class="empty-tip">
        {{ sceneGreeting }}
      </div>

      <div v-if="loading && !streamingMsgId" class="loading-tip">
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
import { agentAsk, agentFeedback as submitFeedback } from '@/api/agent'
import ChatBubble from '@/components/chatBubble.vue'
import MessageInput from '@/components/messageInput.vue'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const messageList = ref<HTMLDivElement | null>(null)
const loading = ref(false)
const sessionId = ref<number | null>(null)
const isFirstSend = ref(true)
const companyId = ref<number>(0)
const deptId = ref<number | undefined>(undefined)
const streamingMsgId = ref<string | null>(null)

const scene = (route.params.scene as string) || 'default'
const isAgentScene = scene === 'default'

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
    default: { title: '智能差旅 Agent', greeting: '欢迎使用智能差旅 Agent！我可以帮您查询政策、比价、合规校验，有什么需要吗？' },
  }
  return map[scene] || map.default
})

const sceneTitle = computed(() => sceneInfo.value!.title)
const sceneGreeting = computed(() => sceneInfo.value!.greeting)

// 加载用户信息（获取 companyId）
const loadUserInfo = async () => {
  try {
    const res = await request.get('/users/userInfo')
    if (res.data) {
      companyId.value = res.data.companyId || 0
      deptId.value = res.data.deptId || undefined
    }
  } catch (err) {
    console.error('获取用户信息失败', err)
  }
}

watch(() => route.params.scene, (newScene, oldScene) => {
  if (newScene && newScene !== oldScene) {
    chatStore.clearMessages()
    sessionId.value = null
    isFirstSend.value = true
    streamingMsgId.value = null

    if (oldScene) {
      localStorage.removeItem(`session_${oldScene}`)
    }

    const savedSid = localStorage.getItem(`session_${newScene}`)
    if (savedSid) {
      sessionId.value = parseInt(savedSid)
      loadHistory(sessionId.value)
      isFirstSend.value = false
    }
  }
}, { immediate: false })

onMounted(async () => {
  await loadUserInfo()

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

// ======================== Agent 模式 ========================

const sendAgentMessage = async (content: string) => {
  streamingMsgId.value = chatStore.addMessage({
    role: 'assistant',
    content: '',
    isStreaming: true,
    showThinking: false,
  })

  try {
    const agentResp = await agentAsk({
      question: content,
      companyId: companyId.value,
      deptId: deptId.value,
    })

    const hasSteps = agentResp.steps && agentResp.steps.length > 0

    chatStore.updateMessage(streamingMsgId.value, {
      content: agentResp.answer,
      steps: agentResp.steps,
      showThinking: hasSteps,
      isStreaming: false,
    })
  } catch (err: any) {
    console.error('Agent 请求失败', err)
    let errMsg = '抱歉，Agent 服务异常，请稍后再试'
    if (err.response?.data?.message) errMsg = err.response.data.message
    chatStore.updateMessage(streamingMsgId.value!, {
      content: errMsg,
      isStreaming: false,
    })
  } finally {
    streamingMsgId.value = null
    scrollToBottom()
  }
}

// ======================== 原有对话模式（场景预订） ========================

const sendDialogMessage = async (content: string, selectedSolutionIndex?: number) => {
  const payload: any = {
    scene: scene.toUpperCase(),
    userInput: content,
    newSession: isFirstSend.value,
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
        summary: sol.solutionName || `${sol.flightNo || ''} - ${sol.airline || ''} - ${sol.cabinClass || ''}`,
        features: sol.features || `${sol.departureTime ? sol.departureTime.substring(11, 16) : ''} 起飞 | ${sol.arrivalTime ? sol.arrivalTime.substring(11, 16) : ''} 到达 | ${sol.cabinClass || ''} | ${sol.channel || '未知渠道'}`,
        price: sol.price || 0,
      }))
    } else {
      replyContent = response.message || JSON.stringify(response)
    }
  }

  chatStore.addMessage({
    role: 'assistant',
    content: replyContent,
    solutions,
    selectedSolutionIndex: selectedSolutionIndexResponse,
  })

  isFirstSend.value = false
}

// ======================== 发送消息入口 ========================

const sendMessage = async (content: string, selectedSolutionIndex?: number) => {
  if (!content.trim()) return

  const userMsg = { role: 'user' as const, content }
  chatStore.addMessage(userMsg)
  scrollToBottom()

  loading.value = true

  try {
    if (isAgentScene && selectedSolutionIndex === undefined) {
      await sendAgentMessage(content)
    } else {
      await sendDialogMessage(content, selectedSolutionIndex)
    }
  } catch (err: any) {
    console.error('对话失败', err)
    let errMsg = '抱歉，服务异常，请稍后再试'
    if (err.response?.data?.message) {
      errMsg = err.response.data.message
    }
    chatStore.addMessage({
      role: 'assistant',
      content: errMsg,
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// ======================== 交互事件 ========================

const handleSelectSolution = async (index: number) => {
  const lastMessage = chatStore.messages[chatStore.messages.length - 1]
  if (!lastMessage || !lastMessage.solutions) return

  chatStore.updateMessage(lastMessage.id, {
    selectedSolutionIndex: index,
  })

  const selectedSolution = lastMessage.solutions.find(s => s.index === index)
  if (!selectedSolution) return

  const confirmMsg = `我选择方案 ${index}：${selectedSolution.summary}（¥${selectedSolution.price}）`

  await sendMessage(confirmMsg, index)
}

const handleToggleThinking = (msgId: string) => {
  const msg = chatStore.messages.find(m => m.id === msgId)
  if (msg) {
    chatStore.updateMessage(msgId, { showThinking: !msg.showThinking })
  }
}

const handleFeedback = (msgId: string, type: 'positive' | 'negative') => {
  chatStore.setFeedback(msgId, type)

  const msg = chatStore.messages.find(m => m.id === msgId)
  const lastUserMsg = [...chatStore.messages].reverse().find(m => m.role === 'user')

  if (lastUserMsg && msg) {
    submitFeedback(
      lastUserMsg.content,
      type === 'positive',
      `session_${scene}`,
    ).catch(err => console.error('提交反馈失败', err))
  }
}

const handleUploadSuccess = (filename: string) => {
  chatStore.addMessage({
    role: 'assistant',
    content: `文件 "${filename}" 已上传成功！请告诉我您想问关于这个文件的问题～`,
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
      selectedSolutionIndex: msg.selectedSolutionIndex,
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
