<template>
  <div class="message-input">
    <van-field
      v-model="inputText"
      type="textarea"
      rows="2"
      autosize
      :placeholder="placeholderText"
      @keyup.enter="handleSend"
    >
      <template #button>
        <van-button
          type="primary"
          size="small"
          :loading="sending"
          @click="handleSend"
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
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { showToast } from 'vant'
import { useRoute } from 'vue-router'

const emit = defineEmits<{
  send: [content: string]
  uploadSuccess: [filename: string]
}>()

const route = useRoute()
const sceneValue = (route.params.scene as string) || 'default'

const inputText = ref('')
const sending = ref(false)
const fileList = ref([])
const isRecording = ref(false)
let mediaRecorder: MediaRecorder | null = null

const placeholderText = computed(() => {
  const map: Record<string, string> = {
    default: '问我任何差旅问题...（Agent 模式）',
    flight: '输入您的机票需求...',
    hotel: '输入您的酒店需求...',
    train: '输入您的火车票需求...',
    car: '输入您的用车需求...',
  }
  return map[sceneValue] || '问我任何差旅问题...'
})

const handleSend = () => {
  if (!inputText.value.trim()) return

  const content = inputText.value.trim()
  inputText.value = ''
  emit('send', content)
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
      emit('uploadSuccess', file.name)
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
