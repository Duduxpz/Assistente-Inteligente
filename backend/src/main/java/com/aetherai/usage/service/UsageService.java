package com.aetherai.usage.service;

import com.aetherai.ai.AIResponse;
import com.aetherai.usage.dto.UsageEntry;
import com.aetherai.usage.dto.UsageSummary;
import com.aetherai.usage.dto.UsageTotals;
import com.aetherai.usage.entity.UsageLogEntity;
import com.aetherai.usage.repository.UsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UsageService {

	private final UsageRepository usageRepository;

	public UsageService(UsageRepository usageRepository) {
		this.usageRepository = usageRepository;
	}

	@Transactional
	public void recordCompletion(UUID userId, AIResponse response) {
		usageRepository.save(new UsageLogEntity(
				userId,
				response.modelId(),
				"chat.completion",
				response.inputTokens() == null ? 0 : response.inputTokens(),
				response.outputTokens() == null ? 0 : response.outputTokens()));
	}

	@Transactional(readOnly = true)
	public UsageSummary forUser(UUID userId) {
		UsageTotals totals = usageRepository.totalsForUser(userId);
		var activity = usageRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(UsageEntry::from)
				.toList();
		return new UsageSummary(totals, activity);
	}

	@Transactional(readOnly = true)
	public UsageTotals forAllUsers() {
		return usageRepository.totalsForAllUsers();
	}
}
