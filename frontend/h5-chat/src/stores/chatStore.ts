import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AgentStep } from '@/api/agent'

export interface Solution {
  summary: string
  features: string
  price: number
  index: number
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  file?: { name: string; url?: string }
  loading?: boolean
  isStreaming?: boolean
  solutions?: Solution[]
  selectedSolutionIndex?: number
  /** Agent 思考步骤 */
  steps?: AgentStep[]
  /** 是否显示思考过程 */
  showThinking?: boolean
  /** 反馈状态 */
  feedback?: 'positive' | 'negative' | null
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const currentQuestion = ref<string>('')
  const isAgentMode = ref(true)

  const addMessage = (msg: Omit<Message, 'id' | 'timestamp'>) => {
    const id = Date.now() + Math.random().toString(36)
    messages.value.push({
      ...msg,
      id,
      timestamp: new Date(),
    })
    return id
  }

  const updateMessage = (id: string, partial: Partial<Message>) => {
    const msg = messages.value.find(m => m.id === id)
    if (msg) Object.assign(msg, partial)
  }

  const clearMessages = () => {
    messages.value = []
  }

  const setFeedback = (messageId: string, feedback: 'positive' | 'negative') => {
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) msg.feedback = feedback
  }

  return {
    messages,
    currentQuestion,
    isAgentMode,
    addMessage,
    updateMessage,
    clearMessages,
    setFeedback,
  }
})
