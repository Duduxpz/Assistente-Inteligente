package com.aetherai;

import com.aetherai.ai.AIProviderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AIProviderProperties.class)
public class AetherAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AetherAiBackendApplication.class, args);
	}
}
