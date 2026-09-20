
## Ollama 本地镜像创建

website: http://localhost

- 映射本地已有大模型

```bash
docker run --name ollama -d -p 11434:11434 -v D:\AI\ollama\models:/root/.ollama ollama/ollama
```


