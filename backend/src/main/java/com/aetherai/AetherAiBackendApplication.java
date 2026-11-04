package com.aetherai;

import com.aetherai.ai.AIProviderProperties;
import com.aetherai.ai.ModelPlanProperties;
import com.aetherai.auth.service.EmailProviderProperties;
import com.aetherai.billing.BillingProperties;
import com.aetherai.rag.EmbeddingProperties;
import com.aetherai.rag.RagProperties;
import com.aetherai.billing.MercadoPagoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		AIProviderProperties.class,
		ModelPlanProperties.class,
		EmailProviderProperties.class,
		MercadoPagoProperties.class,
		BillingProperties.class,
		EmbeddingProperties.class,
		RagProperties.class
})
public class AetherAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AetherAiBackendApplication.class, args);
	}
}
