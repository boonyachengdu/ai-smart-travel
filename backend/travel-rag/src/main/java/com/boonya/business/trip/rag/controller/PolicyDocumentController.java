package com.boonya.business.trip.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.common.models.dialog.DialogRequest;
import com.boonya.business.trip.common.models.rag.PolicyDocumentQueryRequest;
import com.boonya.business.trip.rag.service.PolicyDocumentService;
import com.boonya.business.trip.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/policy")
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentService policyDocumentService;
    private final RagService ragService;

    @PostMapping("/page")
    public Response<Page<PolicyDocument>> page(@RequestBody PolicyDocumentQueryRequest request) {
        LambdaQueryWrapper<PolicyDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyDocument::getEnabled, true);

        UserHolder userHolder = UserHolder.get();
        if (userHolder == null) {
            return Response.error("用户未登录");
        }

        if (!userHolder.isSuperAdmin()) {
            Long currentCompanyId = userHolder.getCompanyId();
            Long currentDeptId = userHolder.getDeptId();

            wrapper.eq(PolicyDocument::getCompanyId, currentCompanyId);

            if (currentDeptId != null) {
                wrapper.and(w -> w
                        .eq(PolicyDocument::getDeptId, currentDeptId)
                        .or()
                        .isNull(PolicyDocument::getDeptId)
                );
            }
        } else {
            if (request.getCompanyId() != null) {
                wrapper.eq(PolicyDocument::getCompanyId, request.getCompanyId());
            }
            if (request.getDeptId() != null) {
                wrapper.eq(PolicyDocument::getDeptId, request.getDeptId());
            }
        }

        wrapper.orderByDesc(PolicyDocument::getUpdateTime);

        Page<PolicyDocument> page = policyDocumentService.page(
                new Page<>(request.getPage(), request.getSize()),
                wrapper
        );

        return Response.ok(page);
    }

    /**
     * 保存政策矢量数据
     */
    @PostMapping
    public Response<Void> savePolicy(@RequestBody PolicyDocument doc) {
        try {
            UserHolder userHolder = UserHolder.get();
            if (userHolder == null) {
                return Response.error("用户未登录");
            }

            if (!userHolder.isSuperAdmin()) {
                doc.setCompanyId(userHolder.getCompanyId());
                doc.setDeptId(null);
            } else {
                if (doc.getCompanyId() == null) {
                    return Response.error("管理员必须指定companyId");
                }
            }

            policyDocumentService.save(doc);
            return Response.ok();
        } catch (SecurityException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to save policy", e);
            return Response.error(e.getMessage());
        }
    }

    /**
     * 上传政策文档--生成矢量数据
     */
    @PostMapping("/upload")
    public Response<Void> uploadPolicy(@RequestParam("scene") String scene, @RequestParam("file") MultipartFile file) {
        try {
            UserHolder userContext = UserHolder.get();
            policyDocumentService.upload(
                    file,
                    userContext.getCompanyId(),
                    userContext.getDeptId(),
                    Scene.valueOf(scene)
            );
            return Response.ok();
        } catch (Exception e) {
            log.error("Failed to upload policy doc", e);
            return Response.error(e.getMessage());
        }
    }

    /**
     * 搜索政策（智能检索）
     */
    @GetMapping("/search")
    public Response<List<PolicyDocument>> searchPolicies(PolicyDocumentQueryRequest request) {
        try {
            UserHolder userHolder = UserHolder.get();
            if (userHolder == null) {
                return Response.error("用户未登录");
            }

            if (!userHolder.isSuperAdmin()) {
                request.setCompanyId(userHolder.getCompanyId());
                if (userHolder.getDeptId() != null) {
                    request.setDeptId(userHolder.getDeptId());
                }
            } else {
                if (request.getCompanyId() == null) {
                    return Response.error("管理员必须指定companyId");
                }
            }

            log.info("Searching policies: {}", request);
            List<PolicyDocument> results = ragService.searchPolicies(request);
            return Response.ok(results);
        } catch (SecurityException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to channel policies", e);
            return Response.error(e.getMessage());
        }
    }

    /**
     * 带员工差标校验的问答
     */
    @PostMapping("/check")
    public Response<String> policyCheck(@RequestBody DialogRequest request) {
        try {
            String question = request.getQuestion();
            if (StringUtils.isEmpty(question)) {
                return Response.error("问题不能为空");
            }
            UserHolder userContext = UserHolder.get();
            if (userContext == null) {
                return Response.error("用户未登录");
            }
            Long companyId = userContext.getCompanyId();
            Long deptId = userContext.getDeptId();

            String answer = ragService.queryWithPolicyCheck(question, companyId, deptId);
            return Response.ok(answer);
        } catch (SecurityException e) {
            log.warn("权限拒绝：{}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            log.error("差标校验问答失败", e);
            return Response.error("差标查询失败，请稍后重试");
        }
    }
}
