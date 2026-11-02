package com.aetherai.ai.controller;

import com.aetherai.ai.AIProvider;
import com.aetherai.ai.dto.AIStatusResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AIStatusController {

	private final ObjectProvider<AIProvider> aiProvider;

	public AIStatusController(ObjectProvider<AIProvider> aiProvider) {
		this.aiProvider = aiProvider;
	}

	@GetMapping("/status")
	public AIStatusResponse status() {
		return new AIStatusResponse(aiProvider.getIfAvailable() != null);
	}
}
