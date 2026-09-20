

# 启动所有服务
```bash
docker-compose up -d
```

# 查看日志
```bash
docker-compose logs -f
```

# 停止服务
```bash
docker-compose down
```

# 停止并删除数据卷（谨慎使用）
```bash
docker-compose down -v
```

# 重启服务
`docker-compose restart`