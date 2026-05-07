package com.boonya.business.trip.rag.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 检索质量评估器 —— 计算MRR、NDCG@k、Precision@k等指标
 */
@Slf4j
@Component
public class RetrievalEvaluator {

    /**
     * 计算 MRR (Mean Reciprocal Rank)
     * MRR = 1/|Q| * Σ(1/rank_i)，其中rank_i是第一个相关文档的排名
     */
    public double calculateMRR(List<EvaluationSample> samples) {
        if (samples == null || samples.isEmpty()) return 0.0;

        double sum = 0.0;
        for (EvaluationSample sample : samples) {
            int rank = findFirstRelevantRank(sample.getRetrievedIds(), sample.getRelevantIds());
            if (rank > 0) {
                sum += 1.0 / rank;
            }
        }
        return sum / samples.size();
    }

    /**
     * 计算 Precision@k
     */
    public double calculatePrecisionAtK(List<String> retrievedIds, Set<String> relevantIds, int k) {
        if (retrievedIds == null || relevantIds == null || retrievedIds.isEmpty()) return 0.0;

        long relevant = retrievedIds.stream()
                .limit(k)
                .filter(relevantIds::contains)
                .count();
        return (double) relevant / Math.min(k, retrievedIds.size());
    }

    /**
     * 计算 NDCG@k (Normalized Discounted Cumulative Gain)
     * 使用分级相关度：relevance_map中score越高越相关
     */
    public double calculateNDCG(List<String> retrievedIds, Map<String, Integer> relevanceScores, int k) {
        if (retrievedIds == null || relevanceScores == null || retrievedIds.isEmpty()) return 0.0;

        List<String> topK = retrievedIds.stream().limit(k).collect(Collectors.toList());

        // 计算DCG
        double dcg = 0.0;
        for (int i = 0; i < topK.size(); i++) {
            int rel = relevanceScores.getOrDefault(topK.get(i), 0);
            dcg += rel / Math.log(i + 2); // i+2 because log(1)=0, use log(i+2)
        }

        // 计算IDCG（理想排序下的DCG）
        List<Integer> sortedRelevance = relevanceScores.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .collect(Collectors.toList());

        double idcg = IntStream.range(0, sortedRelevance.size())
                .mapToDouble(i -> sortedRelevance.get(i) / Math.log(i + 2))
                .sum();

        return idcg == 0 ? 0.0 : dcg / idcg;
    }

    /**
     * 计算 Recall@k
     */
    public double calculateRecallAtK(List<String> retrievedIds, Set<String> relevantIds, int k) {
        if (retrievedIds == null || relevantIds == null || relevantIds.isEmpty()) return 0.0;

        long recalled = retrievedIds.stream()
                .limit(k)
                .filter(relevantIds::contains)
                .count();
        return (double) recalled / relevantIds.size();
    }

    /**
     * 批量评估并输出报告
     */
    public EvaluationReport evaluate(List<EvaluationSample> samples) {
        double mrr = calculateMRR(samples);

        double avgPrecision3 = samples.stream()
                .mapToDouble(s -> calculatePrecisionAtK(s.getRetrievedIds(), s.getRelevantIds(), 3))
                .average().orElse(0.0);

        double avgPrecision5 = samples.stream()
                .mapToDouble(s -> calculatePrecisionAtK(s.getRetrievedIds(), s.getRelevantIds(), 5))
                .average().orElse(0.0);

        double avgRecall3 = samples.stream()
                .mapToDouble(s -> calculateRecallAtK(s.getRetrievedIds(), s.getRelevantIds(), 3))
                .average().orElse(0.0);

        log.info("检索评估报告: MRR={:.4f}, P@3={:.4f}, P@5={:.4f}, R@3={:.4f}, 样本数={}",
                mrr, avgPrecision3, avgPrecision5, avgRecall3, samples.size());

        return EvaluationReport.builder()
                .mrr(mrr)
                .precisionAt3(avgPrecision3)
                .precisionAt5(avgPrecision5)
                .recallAt3(avgRecall3)
                .sampleCount(samples.size())
                .build();
    }

    private int findFirstRelevantRank(List<String> retrievedIds, Set<String> relevantIds) {
        for (int i = 0; i < retrievedIds.size(); i++) {
            if (relevantIds.contains(retrievedIds.get(i))) {
                return i + 1; // 1-indexed
            }
        }
        return -1;
    }
}
