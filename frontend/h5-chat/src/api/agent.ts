import request from '@/utils/request'

export interface AgentStep {
  iteration: number
  thought: string
  action: string
  actionInput: string
  observation: string
  timestamp: string
}

export interface AgentResponse {
  answer: string
  steps: AgentStep[]
  totalIterations: number
  contextUsed: string
  forcedStop: boolean
}

export interface AgentAskParams {
  question: string
  companyId: number
  deptId?: number
}

/**
 * Agent 问答（ReAct 自主推理+工具调用）
 */
export async function agentAsk(params: AgentAskParams): Promise<AgentResponse> {
  const res = await request.post('/agent/ask', null, { params })
  return res.data
}

/**
 * Agent 增强检索
 */
export async function agentSearch(query: string, companyId: number, deptId?: number, topK = 5) {
  const res = await request.get('/agent/search', {
    params: { query, companyId, deptId, topK },
  })
  return res.data
}

/**
 * 提交反馈（点赞/踩）
 */
export async function agentFeedback(query: string, positive: boolean, sessionId: string) {
  const res = await request.post('/agent/feedback', null, {
    params: { query, positive, sessionId },
  })
  return res.data
}

/**
 * SSE 流式聊天（Agent 模式）
 * 返回 AbortController 用于取消请求
 */
export function agentAskStream(
  params: AgentAskParams,
  onChunk: (chunk: string) => void,
  onDone: () => void,
  onError: (err: Error) => void,
): AbortController {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('token')

  const qs = new URLSearchParams({
    question: params.question,
    companyId: String(params.companyId),
  })
  if (params.deptId) qs.set('deptId', String(params.deptId))

  const controller = new AbortController()

  fetch(`${baseURL}/dialog/chat/stream?${qs.toString()}`, {
    method: 'GET',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
    },
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body?.getReader()
      if (!reader) throw new Error('Stream not supported')

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          onDone()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data && data !== '[DONE]') {
              onChunk(data)
            }
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError(err)
      }
    })

  return controller
}
