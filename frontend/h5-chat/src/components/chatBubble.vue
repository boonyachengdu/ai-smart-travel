<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdownRender'
import type { Solution } from '@/stores/chatStore'

interface Props {
  role: 'user' | 'assistant'
  content: string
  solutions?: Solution[]
  selectedSolutionIndex?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  selectSolution: [index: number]
}>()

const renderedContent = computed(() => {
  return renderMarkdown(props.content)
})

const isUser = computed(() => props.role === 'user')

const hasSolutions = computed(() => {
  return props.solutions && props.solutions.length > 0
})

const handleSelectSolution = (index: number) => {
  emit('selectSolution', index)
}
</script>

<template>
  <div class="chat-bubble" :class="isUser ? 'user' : 'assistant'">
    <div class="avatar">
      {{ isUser ? '🤗' : '🤖' }}
    </div>
    <div class="message-container">
      <div class="content" v-html="renderedContent"></div>

      <div v-if="hasSolutions" class="solutions-container">
        <div
            v-for="solution in solutions"
            :key="solution.index"
            class="solution-card"
            :class="{ selected: selectedSolutionIndex === solution.index }"
            @click="handleSelectSolution(solution.index)"
        >
          <div class="solution-header">
            <span class="solution-title">{{ solution.summary }}</span>
            <span class="solution-index">#{{ solution.index }}</span>
          </div>
          <div class="solution-features">{{ solution.features }}</div>
          <div class="solution-price">¥{{ solution.price }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-bubble {
  display: flex;
  margin-bottom: 16px;
  gap: 12px;
}

.chat-bubble.user {
  flex-direction: row-reverse;
}

.avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-container {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content {
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  text-align: left;
  word-wrap: break-word;
  white-space: pre-wrap;
  line-height: 1.6;
}

.chat-bubble.user .content {
  background: #0084ff;
  color: white;
}

.chat-bubble.assistant .content {
  background: white;
  color: #333;
}

.solutions-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.solution-card {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.solution-card:hover {
  border-color: #0084ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 132, 255, 0.2);
}

.solution-card.selected {
  border-color: #0084ff;
  background: #f0f7ff;
}

.solution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.solution-title {
  font-weight: bold;
  font-size: 15px;
  color: #333;
}

.solution-index {
  background: #0084ff;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.solution-features {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.solution-price {
  font-size: 16px;
  font-weight: bold;
  color: #ff6b00;
}
</style>
