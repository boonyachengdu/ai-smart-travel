package com.boonya.business.trip.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.entity.PolicyDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * 差旅政策知识库服务ss
 * 基于 Spring AI 1.0.0-M6.1 + Alibaba Tongyi + pgvector
 *
 * @description: 支持多格式文件上传、向量化存储
 */
public interface PolicyDocumentService extends IService<PolicyDocument> {

    void store(PolicyDocument doc);

    void upload(MultipartFile file, Long companyId, Long deptId, Scene scene);

    String readFileContent(MultipartFile file) throws Exception;
}
