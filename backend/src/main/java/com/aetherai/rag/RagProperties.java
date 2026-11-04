package com.aetherai.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rag")
public record RagProperties(int chunkSize, int chunkOverlap) {
}
