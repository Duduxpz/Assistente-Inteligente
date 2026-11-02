package com.aetherai.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.mercadopago")
public record MercadoPagoProperties(String accessToken, String webhookSecret, String frontendBaseUrl) {
}
