// src/utils/markdownRender.ts
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript' // 示例，添加常用语言
import 'highlight.js/styles/github.css' // 样式文件

// 注册语言（按需加载，避免打包太大）
hljs.registerLanguage('javascript', javascript)
// 可继续添加其他语言：python, java, bash 等
// hljs.registerLanguage('python', python)

const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    highlight: (str: string, lang: string) => {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return hljs.highlight(str, { language: lang }).value
            } catch (__) {}
        }
        return '' // 默认转义
    },
})

export function renderMarkdown(content: string): string {
    return md.render(content)
}