package com.boonya.business.trip.rag.service.impl;

import com.boonya.business.trip.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty");
        }

        try {
            log.debug("Generating embedding for text (length: {})", text.length());
            float[] embedding = embeddingModel.embed(text);
            log.debug("Generated embedding with dimension: {}", embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Error generating embedding", e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Input texts cannot be null or empty");
        }

        try {
            log.debug("Generating batch embeddings for {} texts", texts.size());

            List<String> validTexts = texts.stream()
                    .filter(t -> t != null && !t.trim().isEmpty())
                    .collect(Collectors.toList());

            if (validTexts.isEmpty()) {
                throw new IllegalArgumentException("No valid texts to generate embeddings");
            }

            return embeddingModel.embed(validTexts);

        } catch (Exception e) {
            log.error("Error generating batch embeddings", e);
            throw new RuntimeException("Failed to generate batch embeddings: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimension() {
        try {
            float[] testEmbedding = generateEmbedding("test");
            return testEmbedding.length;
        } catch (Exception e) {
            log.warn("Failed to get embedding dimension, defaulting to 1536");
            return 1536;
        }
    }
}
