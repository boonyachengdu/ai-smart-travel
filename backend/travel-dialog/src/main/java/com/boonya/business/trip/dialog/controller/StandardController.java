package com.boonya.business.trip.dialog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Standard;
import com.boonya.business.trip.common.models.order.request.StandardQueryRequest;
import com.boonya.business.trip.dialog.service.StandardService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/standard")
@RequiredArgsConstructor
public class StandardController {

    private final StandardService standardService;

    /**
     * 分页查询差标标准列表
     */
    @PostMapping("/page")
    public Response<Page<Standard>> page(@RequestBody StandardQueryRequest request) {
        LambdaQueryWrapper<Standard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Standard::getDeleted, 0);
        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Standard::getCompanyId, userHolder.getCompanyId());
        }

        if (StringUtils.hasText(request.getName())) {
            wrapper.like(Standard::getName, request.getName());
        }

        wrapper.orderByDesc(Standard::getUpdateTime);

        Page<Standard> page = standardService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据ID查询单个差标标准
     */
    @GetMapping("/{id}")
    public Response<Standard> getById(@PathVariable("id") Long id) {
        Standard standard = standardService.getById(id);
        return standard != null ? Response.ok(standard) : Response.error("差标标准不存在");
    }

    /**
     * 新增差标标准
     */
    @PostMapping
    public Response<Long> save(@RequestBody Standard standard) {
        boolean success = standardService.save(standard);
        return success ? Response.ok(standard.getId()) : Response.error("新增失败");
    }

    /**
     * 修改差标标准
     */
    @PutMapping("/{id}")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Standard standard) {
        standard.setId(id);
        boolean success = standardService.updateById(standard);
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 删除差标标准（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = standardService.removeById(id);
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    // StandardController.java - 新增批量初始化和场景查询接口

    /**
     * 批量初始化差标标准
     */
    @PostMapping("/batch-init/{companyId}")
    public Response<Boolean> batchInit(@PathVariable("companyId") Long companyId) {
        return standardService.batchInit(companyId) ? Response.ok(true) : Response.error("批量初始化失败");
    }

    /**
     * 根据场景查询差标
     */
    @GetMapping("/scene/{companyId}/{scene}")
    public Response<Standard> getByScene(@PathVariable("companyId") Long companyId,
                                         @PathVariable("scene") String scene) {
        // 根据场景智能匹配差标（可根据用户角色自动选择）
        List<Standard> standards = standardService.getStandardsByCompanyId(companyId);

        if (standards.isEmpty()) {
            return Response.error("企业未设置差标标准");
        }

        // 默认返回第一个（或根据用户角色匹配）
        return Response.ok(standards.get(0));
    }

}
