import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Solution {
    summary: string
    features: string
    price: string
    index: number
}

export interface Message {
    id: string
    role: 'user' | 'assistant'
    content: string
    timestamp: Date
    file?: { name: string; url?: string }
    loading?: boolean
    solutions?: Solution[]
    selectedSolutionIndex?: number
}

export const useChatStore = defineStore('chat', () => {
    const messages = ref<Message[]>([])
    const currentQuestion = ref<string>('')

    const addMessage = (msg: Omit<Message, 'id' | 'timestamp'>) => {
        const id = Date.now() + Math.random().toString(36)
        messages.value.push({
            ...msg,
            id,
            timestamp: new Date(),
        })
    }

    const updateMessage = (id: string, partial: Partial<Message>) => {
        const msg = messages.value.find(m => m.id === id)
        if (msg) Object.assign(msg, partial)
    }

    const clearMessages = () => {
        messages.value = []
    }

    return {
        messages,
        currentQuestion,
        addMessage,
        updateMessage,
        clearMessages,
    }
})
