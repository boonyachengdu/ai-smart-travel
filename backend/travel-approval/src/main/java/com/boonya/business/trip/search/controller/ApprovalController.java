package com.boonya.business.trip.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.AuditStatus;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.Approval;
import com.boonya.business.trip.common.models.order.request.ApprovalQueryRequest;
import com.boonya.business.trip.search.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 分页查询审批记录列表
     */
    @PostMapping("/page")
    @Operation(summary = "分页查询审批列表")
    public Response<Page<Approval>> page(@RequestBody ApprovalQueryRequest request) {
        LambdaQueryWrapper<Approval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Approval::getDeleted, 0);

        // 过滤用户企业
        UserHolder userHolder = UserHolder.get();
        if (!userHolder.isSuperAdmin()){
            wrapper.eq(Approval::getCompanyId, userHolder.getCompanyId());
        }

        if (request.getOrderNo() != null) {
            wrapper.eq(Approval::getOrderNo, request.getOrderNo());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Approval::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(Approval::getUpdateTime);

        Page<Approval> page = approvalService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 根据 ID 查询单个审批记录（包含出行项）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询审批详情")
    public Response<Approval> getById(@PathVariable("id") Long id) {
        Approval approval = approvalService.getById(id);
        if (approval == null) {
            return Response.error("审批记录不存在");
        }
        return Response.ok(approval);
    }

    /**
     * 新增审批记录（不带动行项，保持原有接口）
     */
    @PostMapping("save")
    @Operation(summary = "新增审批记录")
    public Response<Long> save(@RequestBody Approval approval) {
        UserHolder userContext = UserHolder.get();
        approval.setCompanyId(userContext.getCompanyId());
        approval.setApplicantId(userContext.getEmployeeId());
        boolean success = approvalService.save(approval);
        return success ? Response.ok(approval.getId()) : Response.error("新增失败");
    }

    /**
     * 修改审批记录
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改审批记录")
    public Response<Boolean> update(@PathVariable("id") Long id, @RequestBody Approval approval) {
        approval.setId(id);
        boolean success = approvalService.updateById(approval);
        return success ? Response.ok(true) : Response.error("修改失败");
    }

    /**
     * 审批操作（通过/拒绝）
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批操作")
    public Response<Boolean> approve(@PathVariable("id") Long id, @RequestBody Approval approval) {
        if (AuditStatus.APPROVED.equals(approval.getStatus()) && AuditStatus.REJECTED.equals(approval.getStatus())) {
            return Response.error("审批类型枚举不正确!");
        }

        Approval temp = approvalService.getById(id);
        if (temp == null) {
            return Response.error("审批记录不存在");
        }

        try {
            temp.setRemark(approval.getRemark());
            temp.setStatus(approval.getStatus());
            boolean success = approvalService.updateApproveOrReject(temp);
            return success ? Response.ok(true) : Response.error("审批操作失败");
        } catch (Exception e) {
            log.error("审批操作失败", e);
            return Response.error("审批操作失败");
        }
    }

    /**
     * 审批操作（通过/拒绝）
     */
    @PutMapping("/callback/{id}/approve")
    @Operation(summary = "审批操作")
    public Response<Boolean> approveCallback(@PathVariable("id") Long id, @RequestBody Approval approval) {
        if (!AuditStatus.APPROVED.equals(approval.getStatus()) && !AuditStatus.REJECTED.equals(approval.getStatus())) {
            return Response.error("审批类型枚举不正确!");
        }

        Approval temp = approvalService.getById(id);
        if (temp == null) {
            return Response.error("审批记录不存在");
        }

        try {
            temp.setRemark(approval.getRemark());
            temp.setStatus(approval.getStatus());
            boolean success = approvalService.updateApproveOrRejectCallback(temp);
            return success ? Response.ok(true) : Response.error("审批操作失败");
        } catch (Exception e) {
            log.error("审批操作失败", e);
            return Response.error("审批操作失败");
        }
    }

    /**
     * 删除审批记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除审批记录")
    public Response<Boolean> delete(@PathVariable("id") Long id) {
        boolean success = approvalService.removeById(id);
        return success ? Response.ok(true) : Response.error("删除失败");
    }

    /**
     * 查询待审批记录列表
     */
    @PostMapping("/unApprovalList")
    @Operation(summary = "查询待审批记录列表")
    public Response<List<Approval>> getUnApprovalList() {
        return Response.ok(approvalService.getUnApprovalList());
    }

    /**
     * 查询待审批数量
     */
    @GetMapping("/pendingApprovals")
    public Response<Long> pendingApprovals() {
        return Response.ok(approvalService.pendingApprovals());
    }
}
