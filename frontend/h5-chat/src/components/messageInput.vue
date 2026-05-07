<template>
  <div class="message-input">
    <van-field
        v-model="inputText"
        type="textarea"
        rows="2"
        autosize
        placeholder="问我任何差旅问题..."
        @keyup.enter="sendText"
    >
      <template #button>
        <van-button
            type="primary"
            size="small"
            :loading="sending"
            @click="sendText"
        >
          发送
        </van-button>
      </template>
    </van-field>

    <div class="tools">
      <van-uploader
          v-model="fileList"
          :max-count="1"
          :max-size="10 * 1024 * 1024"
          accept=".txt,.pdf,.doc,.docx,.xls,.xlsx"
          :before-read="beforeUpload"
          :after-read="afterUpload"
      >
        <van-icon name="attachment" size="24" />
      </van-uploader>

      <van-icon
          name="microphone"
          size="24"
          :color="isRecording ? 'red' : '#666'"
          @click="toggleVoice"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import request from '@/utils/request'
import { showToast } from 'vant'
import {useRoute} from "vue-router";

const chatStore = useChatStore()
const inputText = ref('')
const sending = ref(false)
const fileList = ref([])
const isRecording = ref(false)
let mediaRecorder: MediaRecorder | null = null

// 关键变量：首次发送时创建新会话
const isFirstSend = ref(true)

const route = useRoute()
const sceneValue = (route.params.scene as string) || 'default'

const sendText = async () => {
  if (!inputText.value.trim()) return

  const msg = {
    role: 'user' as const,
    content: inputText.value.trim(),
  }
  chatStore.addMessage(msg)

  sending.value = true
  inputText.value = ''

  try {
    const payload = {
      userInput: msg.content,
      newSession: isFirstSend.value,
      scene: sceneValue.toUpperCase(),
      // 如果已有 sessionId，可在此添加（当前后端会自动管理）
      // sessionId: chatStore.sessionId || undefined,
    }

    const res = await request.post('/dialog/chat/session', payload)

    // 打印完整返回，便于调试（可上线后删除）
    console.log('对话返回完整结构：', JSON.stringify(res, null, 2))

    // 解析后端返回的 response
    const response = res.data?.response || {}

    let replyContent = '暂无回复'

    if (response.status === 'clarify' && response.clarifyQuestions?.length > 0) {
      // 澄清问题：格式化为列表
      replyContent = '请补充以下信息：\n' +
          response.clarifyQuestions.map(q => `- ${q}`).join('\n')
    } else if (response.status === 'complete' && response.order) {
      // 订单生成成功
      replyContent = `订单已生成！\n` +
          `订单号：${response.order.orderNo || '生成中'}\n` +
          `金额：¥${response.order.amount || '待确认'}\n` +
          `状态：${response.order.status || '待支付'}`
    } else if (response.status === 'violation') {
      // 违规
      replyContent = `订单违反差标规则：${response.violationReason || '具体原因未知'}`
    } else if (response.message) {
      // 默认文本回复
      replyContent = response.message
    } else {
      // 兜底：字符串化整个 response
      replyContent = JSON.stringify(response, null, 2)
    }

    chatStore.addMessage({
      role: 'assistant',
      content: replyContent
    })

    // 第一次发送成功后，标记为 false（后续都续会话）
    isFirstSend.value = false
  } catch (err: any) {
    console.error('对话请求失败', err)
    let errMsg = '抱歉，服务异常，请稍后再试'
    if (err.response?.data?.message) {
      errMsg = err.response.data.message
    }
    chatStore.addMessage({
      role: 'assistant',
      content: errMsg
    })
  } finally {
    sending.value = false
  }
}

const beforeUpload = (file: File) => {
  const allow = ['txt', 'pdf', 'doc', 'docx', 'xls', 'xlsx']
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!allow.includes(ext || '')) {
    showToast('仅支持 txt/pdf/word/excel 文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('文件大小不能超过 10MB')
    return false
  }
  return true
}

const afterUpload = async (fileItem: any) => {
  const file = fileItem.file
  try {
    const formData = new FormData()
    formData.append('file', file)

    const res = await request.post('/rag/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    if (res.success) {
      inputText.value = `已上传文件：${file.name}，请问相关问题`
    }
  } catch (err) {
    showToast('文件上传失败')
  }
}

const toggleVoice = async () => {
  if (isRecording.value) {
    mediaRecorder?.stop()
    isRecording.value = false
  } else {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream)
      const chunks: Blob[] = []

      mediaRecorder.ondataavailable = e => chunks.push(e.data)
      mediaRecorder.onstop = () => {
        showToast('语音录制完成，待接入转文字功能')
      }

      mediaRecorder.start()
      isRecording.value = true
    } catch (err) {
      showToast('无法访问麦克风')
    }
  }
}
</script>

<style scoped>
.message-input {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 12px;
  border-top: 1px solid #eee;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
}

.tools {
  display: flex;
  gap: 20px;
  margin-top: 8px;
  color: #666;
}
</style>