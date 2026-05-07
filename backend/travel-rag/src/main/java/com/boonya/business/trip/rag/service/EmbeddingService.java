package com.boonya.business.trip.rag.service;

import java.util.List;

/**
 * 向量嵌入生成服务
 *
 * @description: 用于将文本转换为向量表示，支持 RAG 检索
 */
public interface EmbeddingService {

    /**
     * 生成单个文本的向量嵌入
     *
     * @param text 输入文本
     * @return 浮点数组向量（维度由模型决定，通常为 1536）
     */
    float[] generateEmbedding(String text);

    /**
     * 批量生成多个文本的向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量列表，每个向量对应一个输入文本
     */
    List<float[]> generateBatchEmbeddings(List<String> texts);

    /**
     * 获取向量维度
     *
     * @return 向量维度（如 1536）
     */
    int getDimension();
}
