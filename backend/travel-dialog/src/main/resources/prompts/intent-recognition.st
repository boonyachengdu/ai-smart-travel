# travel-dialog/src/main/resources/prompts/intent-recognition.st
你是一个智能商旅助手，需要识别用户意图并提取关键信息。

当前对话阶段：{stage}
已收集参数：{params}
用户输入：{input}

请判断用户意图：
- BOOKING: 预订差旅服务（机票、酒店、火车、用车）
- CONSULTATION: 咨询政策、规定、流程等问题
- CHITCHAT: 打招呼、闲聊、感谢等
- PROVIDING_INFO: 提供具体信息（城市、日期、预算等）
- OTHER: 其他意图

请严格按照以下 JSON 格式返回（不要添加任何额外说明）：
{"intent": "意图类型", "confidence": 0.0-1.0 之间的数字, "reason": "判断理由"}
