package com.boonya.business.trip.rag.controller;

import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.rag.agent.AgentMemoryService;
import com.boonya.business.trip.rag.agent.AgentWorkingMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Agent记忆管理 API —— 供后台管理系统使用
 */
@Slf4j
@RestController
@RequestMapping("/agent/memory")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final AgentMemoryService memoryService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询用户Agent记忆列表
     */
    @PostMapping("/page")
    public Response<Map<String, Object>> page(
            @RequestBody Map<String, Object> params) {

        int pageNum = params.containsKey("pageNum") ? ((Number) params.get("pageNum")).intValue() : 1;
        int pageSize = params.containsKey("pageSize") ? ((Number) params.get("pageSize")).intValue() : 10;

        Set<String> keys = stringRedisTemplate.keys("agent:memory:*");
        if (keys == null) keys = Collections.emptySet();

        List<AgentWorkingMemory> all = new ArrayList<>();
        for (String key : keys) {
            String userId = key.replace("agent:memory:", "");
            AgentWorkingMemory memory = memoryService.load(userId);
            if (memory.getLastActiveTime() != null) {
                all.add(memory);
            }
        }

        all.sort((a, b) -> {
            if (a.getLastActiveTime() == null) return 1;
            if (b.getLastActiveTime() == null) return -1;
            return b.getLastActiveTime().compareTo(a.getLastActiveTime());
        });

        int total = all.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<AgentWorkingMemory> page = from < total ? all.subList(from, to) : Collections.emptyList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", page);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Response.ok(result);
    }

    /**
     * 查看单个用户的Agent记忆详情
     */
    @GetMapping("/{userId}")
    public Response<AgentWorkingMemory> get(@PathVariable String userId) {
        return Response.ok(memoryService.load(userId));
    }

    /**
     * 清除指定用户的Agent记忆
     */
    @DeleteMapping("/{userId}")
    public Response<String> delete(@PathVariable String userId) {
        memoryService.clear(userId);
        return Response.ok("记忆已清除");
    }

    /**
     * 批量清除Agent记忆
     */
    @DeleteMapping("/batch")
    public Response<String> batchDelete(@RequestBody List<String> userIds) {
        for (String userId : userIds) {
            memoryService.clear(userId);
        }
        return Response.ok("已清除 " + userIds.size() + " 条记忆");
    }
}
