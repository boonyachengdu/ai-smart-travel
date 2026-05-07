package com.boonya.business.trip.rag.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.entity.Prompts;
import com.boonya.business.trip.common.models.rag.PromptQueryRequest;
import com.boonya.business.trip.rag.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 提示词控制器
 */
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    /**
     * 分页查询提示词列表
     */
    @PostMapping("/page")
    public Response<Page<Prompts>> page(@RequestBody PromptQueryRequest request) {
        LambdaQueryWrapper<Prompts> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prompts::getDeleted, 0);

        if (request.getEnabled() != null) {
            wrapper.eq(Prompts::getEnabled, request.getEnabled());
        }
        if (StringUtils.isNotBlank(request.getName())) {
            wrapper.like(Prompts::getName, request.getName());
        }

        wrapper.orderByDesc(Prompts::getUpdateTime);

        Page<Prompts> page = promptService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询单个提示词
     */
    @GetMapping("/{id}")
    public Response<Prompts> getById(@PathVariable("id") Long id) {
        Prompts prompt = promptService.getById(id);
        return prompt != null ? Response.ok(prompt) : Response.error("提示词不存在");
    }

    /**
     * 新增提示词
     */
    @PostMapping
    public Response<Boolean> save(@RequestBody Prompts prompt) {
        // 设置默认值
        if (prompt.getEnabled() == null) {
            prompt.setEnabled(true);
        }
        boolean success = promptService.save(prompt);
        return success ? Response.ok(true) : Response.error("新增失败");
    }

    /**
     * 修改提示词
     */
    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Prompts prompt) {
        prompt.setId(id);
        boolean success = promptService.updateById(prompt);
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 删除提示词（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = promptService.removeById(id);
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    /**
     * 启用/禁用提示词
     */
    @PutMapping("/{id}/toggle-enabled")
    public Response<Boolean> toggleEnabled(@PathVariable("id") Long id, @RequestParam Boolean enabled) {
        Prompts prompt = new Prompts();
        prompt.setId(id);
        prompt.setEnabled(enabled);
        boolean success = promptService.updateById(prompt);
        return success ? Response.ok(true) : Response.error("操作失败");
    }
}