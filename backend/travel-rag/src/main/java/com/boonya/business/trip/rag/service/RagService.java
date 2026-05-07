package com.boonya.business.trip.rag.service;

import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.common.models.rag.PolicyDocumentQueryRequest;
import com.boonya.business.trip.rag.search.SearchResult;

import java.util.List;

/**
 * RAG（检索增强生成）服务接口
 *
 * @description: 定义 RAG 服务的标准接口，支持增强搜索管道（查询改写+混合检索+重排序）
 */
public interface RagService {

    /**
     * 使用 RAG 增强查询（返回上下文）
     *
     * @param query 用户查询
     * @param companyId 企业 ID
     * @return 增强上下文，如果没有相关文档则返回 null
     */
    String enhanceWithRag(String query, Long companyId);

    /**
     * 使用增强检索管道获取上下文（查询改写 + 混合检索 + 重排序）
     *
     * @param query 用户查询
     * @param companyId 企业 ID
     * @param deptId 部门 ID（可选）
     * @return 增强上下文
     */
    String enhanceWithRag(String query, Long companyId, Long deptId);

    /**
     * 使用 RAG 生成答案（完整流程）
     *
     * @param query 用户查询
     * @param companyId 企业 ID
     * @param deptId 部门 ID（可选）
     * @return AI 生成的答案
     */
    String generateAnswerWithRag(String query, Long companyId, Long deptId);

    /**
     * 带差标校验的 RAG 问答
     *
     * @param question 用户问题
     * @param companyId 企业 ID
     * @param deptId 部门 ID
     * @return 包含差标校验的回答
     */
    String queryWithStandardCheck(String question, Long companyId, Long deptId);

    String queryWithPolicyCheck(String question, Long companyId, Long deptId);

    /**
     * 快速检查是否合规（仅返回布尔值）
     *
     * @param orderType 订单类型（FLIGHT/HOTEL/TRAIN/CAR）
     * @param amount 金额
     * @param companyId 企业 ID
     * @return 是否合规
     */
    boolean isCompliant(String orderType, Double amount, Long companyId);

    List<PolicyDocument> searchPolicies(PolicyDocumentQueryRequest request);

    /**
     * 从数据库检索相关政策（增强管道：查询改写 + 混合检索 + 重排序）
     *
     * @param query 查询文本
     * @param companyId 企业 ID
     * @param deptId 部门 ID（可选）
     * @param limit 返回数量限制
     * @return 相关政策文档列表
     */
    List<PolicyDocument> searchPolicies(String query, Long companyId, Long deptId, int limit);

    /**
     * 使用增强管道检索（返回原始SearchResult，用于Agent工具调用）
     */
    List<SearchResult> searchWithEnhancedPipeline(String query, Long companyId, Long deptId, int topK);
}
