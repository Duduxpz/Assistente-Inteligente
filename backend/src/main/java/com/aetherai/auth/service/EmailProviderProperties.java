package com.aetherai.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.email.resend")
public record EmailProviderProperties(String apiKey, String from, String frontendUrl) {
}
