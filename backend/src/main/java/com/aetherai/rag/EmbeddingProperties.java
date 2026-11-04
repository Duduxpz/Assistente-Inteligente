package com.aetherai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rag.embedding")
public record EmbeddingProperties(String baseUrl, String apiKey, String model, int dimensions) {
}
