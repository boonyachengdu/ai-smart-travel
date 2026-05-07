# 环境搭建
## Postgres环境搭建包括向量数据库
```
docker run -d --name postgres-pgvector -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=smart_travel -v pgdata:/var/lib/postgresql/data pgvector/pgvector:pg16docker run -d --name postgres-pgvector -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=smart_travel -v pgdata:/var/lib/postgresql/data pgvector/pgvector:pg16
```

## Redis环境搭建

```
docker run -d --name redis -p 6379:6379 redis:latest
```

## Nacos单机环境搭建

```
docker run -d --name nacos -p 8848:8848 -p 9848:9848 -p 9849:9849 -e MODE=standalone nacos/nacos-server:v2.3.2
```

## Sentinel熔断限流
```
docker pull bladex/sentinel-dashboard
docker run --name sentinel-dashboard -p 8080:8080 -d bladex/sentinel-dashboard
```

# 新模块负载均衡

Nacos服务实例发现，不加会导致服务之间互相无法感知
```
<!-- 负载均衡 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
```
## 验证路由配置

http://localhost:9000/actuator/gateway/routes

## 升级到Agent启动执行脚本
```
psql -h localhost -U postgres -d smart_travel -f sql/V2__rag_fulltext_search.sql
```