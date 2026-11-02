package com.aetherai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ai.openrouter")
public record AIProviderProperties(String apiKey, String model, String siteUrl) {
}
