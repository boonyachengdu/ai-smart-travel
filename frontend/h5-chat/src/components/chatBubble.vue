<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdownRender'
import type { Solution, Message } from '@/stores/chatStore'

interface Props {
  role: 'user' | 'assistant'
  content: string
  solutions?: Solution[]
  selectedSolutionIndex?: number
  steps?: Message['steps']
  showThinking?: boolean
  isStreaming?: boolean
  feedback?: 'positive' | 'negative' | null
  messageId?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  selectSolution: [index: number]
  toggleThinking: []
  feedback: [type: 'positive' | 'negative']
}>()

const renderedContent = computed(() => {
  return renderMarkdown(props.content)
})

const isUser = computed(() => props.role === 'user')

const hasSolutions = computed(() => {
  return props.solutions && props.solutions.length > 0
})

const hasSteps = computed(() => {
  return props.steps && props.steps.length > 0
})

const handleSelectSolution = (index: number) => {
  emit('selectSolution', index)
}

const handleToggleThinking = () => {
  emit('toggleThinking')
}

const handleFeedback = (type: 'positive' | 'negative') => {
  emit('feedback', type)
}
</script>

<template>
  <div class="chat-bubble" :class="isUser ? 'user' : 'assistant'">
    <div class="avatar">
      {{ isUser ? '🤗' : '🤖' }}
    </div>
    <div class="message-container">
      <!-- Agent 思考过程（可折叠） -->
      <div v-if="hasSteps && !isUser" class="thinking-panel">
        <div class="thinking-header" @click="handleToggleThinking">
          <span class="thinking-icon">{{ showThinking ? '🔽' : '▶️' }}</span>
          <span class="thinking-label">
            🧠 Agent 思考过程 ({{ steps!.length }} 步)
            <span v-if="steps!.some(s => s.action === 'final_answer')" class="thinking-done">✅</span>
          </span>
        </div>
        <div v-if="showThinking" class="thinking-body">
          <div
            v-for="step in steps"
            :key="step.iteration"
            class="thinking-step"
          >
            <div class="step-header">
              <span class="step-num">Step {{ step.iteration }}</span>
              <span class="step-action">{{ step.action }}</span>
            </div>
            <div class="step-thought">💭 {{ step.thought }}</div>
            <div v-if="step.observation" class="step-observation">
              📋 {{ step.observation.substring(0, 200) }}{{ step.observation.length > 200 ? '...' : '' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 消息内容 -->
      <div class="content" v-html="renderedContent"></div>

      <!-- 流式输出光标 -->
      <span v-if="isStreaming" class="streaming-cursor">|</span>

      <!-- 解决方案卡片 -->
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

      <!-- 反馈按钮（仅 AI 消息，非流式状态） -->
      <div v-if="!isUser && !isStreaming && content" class="feedback-bar">
        <button
          class="feedback-btn"
          :class="{ active: feedback === 'positive' }"
          title="有帮助"
          @click="handleFeedback('positive')"
        >
          {{ feedback === 'positive' ? '👍' : '👍🏻' }}
        </button>
        <button
          class="feedback-btn"
          :class="{ active: feedback === 'negative' }"
          title="不准确"
          @click="handleFeedback('negative')"
        >
          {{ feedback === 'negative' ? '👎' : '👎🏻' }}
        </button>
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
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thinking-panel {
  background: #fff8e1;
  border-radius: 8px;
  border: 1px solid #ffe082;
  overflow: hidden;
}

.thinking-header {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #795548;
  user-select: none;
}

.thinking-header:hover {
  background: #fff3cd;
}

.thinking-icon {
  font-size: 10px;
}

.thinking-label {
  font-weight: 500;
}

.thinking-done {
  margin-left: 4px;
}

.thinking-body {
  padding: 8px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.thinking-step {
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border-left: 3px solid #ff9800;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.step-num {
  font-size: 11px;
  font-weight: 600;
  color: #e65100;
  background: #fff3cd;
  padding: 1px 6px;
  border-radius: 4px;
}

.step-action {
  font-size: 11px;
  color: #795548;
  font-family: monospace;
}

.step-thought {
  font-size: 12px;
  color: #555;
  margin-top: 4px;
}

.step-observation {
  font-size: 11px;
  color: #888;
  margin-top: 4px;
  font-style: italic;
}

.content {
  padding: 12px 16px;
  border-radius: 12px;
  background: white;
  text-align: left;
  word-wrap: break-word;
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

.streaming-cursor {
  font-size: 18px;
  color: #0084ff;
  animation: blink 0.7s infinite;
  align-self: flex-start;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
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

.feedback-bar {
  display: flex;
  gap: 8px;
  padding: 4px 0;
}

.feedback-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
  opacity: 0.6;
}

.feedback-btn:hover {
  opacity: 1;
  background: #f0f0f0;
}

.feedback-btn.active {
  opacity: 1;
  transform: scale(1.15);
}
</style>
