package com.aetherai.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.billing")
public record BillingProperties(boolean testModeEnabled, String testUltraEmail) {
}
