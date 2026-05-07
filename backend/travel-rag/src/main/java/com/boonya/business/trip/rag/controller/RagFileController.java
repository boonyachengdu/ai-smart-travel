package com.boonya.business.trip.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.RagFile;
import com.boonya.business.trip.common.models.rag.RagFileQueryRequest;
import com.boonya.business.trip.rag.service.RagFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 文件管理
 */
@Slf4j
@RestController
@RequestMapping("/rag-file")
@RequiredArgsConstructor
public class RagFileController {

    private final RagFileService ragFileService;

    /**
     * 分页查询文件列表（支持用户/企业隔离）
     */
    @PostMapping("/page")
    public Response<Page<RagFile>> page(@RequestBody RagFileQueryRequest request) {
        LambdaQueryWrapper<RagFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagFile::getDeleted, 0);

        UserHolder userHolder = UserHolder.get();
        if (userHolder == null) {
            return Response.error("用户未登录");
        }

        if (!userHolder.isSuperAdmin()) {
            wrapper.eq(RagFile::getCompanyId, userHolder.getCompanyId());
            if (request.getCompanyId() != null && !request.getCompanyId().equals(userHolder.getCompanyId())) {
                return Response.error("无权查看其他企业的数据");
            }
        } else {
            if (request.getCompanyId() != null) {
                wrapper.eq(RagFile::getCompanyId, request.getCompanyId());
            }
        }

        if (StringUtils.hasText(request.getFileName())) {
            wrapper.like(RagFile::getFileName, request.getFileName());
        }
        if (StringUtils.hasText(request.getMimeType())) {
            wrapper.eq(RagFile::getMimeType, request.getMimeType());
        }

        wrapper.orderByDesc(RagFile::getCreateTime);

        Page<RagFile> page = ragFileService.page(new Page<>(request.getPage(), request.getSize()), wrapper);
        return Response.ok(page);
    }

}
