package com.aetherai;

import com.aetherai.ai.AIProviderProperties;
import com.aetherai.auth.service.EmailProviderProperties;
import com.aetherai.billing.BillingProperties;
import com.aetherai.billing.MercadoPagoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		AIProviderProperties.class,
		EmailProviderProperties.class,
		MercadoPagoProperties.class,
		BillingProperties.class
})
public class AetherAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AetherAiBackendApplication.class, args);
	}
}
