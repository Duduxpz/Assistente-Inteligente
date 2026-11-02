package com.aetherai.plan.service;

import com.aetherai.plan.PlanTier;
import com.aetherai.plan.dto.PlanCatalogResponse;
import com.aetherai.plan.dto.PlanDetails;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PlanCatalogService {

	public PlanCatalogResponse catalog(String currentPlanKey) {
		List<PlanDetails> plans = Arrays.stream(PlanTier.values())
				.map(plan -> new PlanDetails(
						plan.key(),
						plan.displayName(),
						plan.description(),
						plan.monthlyPriceCents(),
						plan.monthlyMessages(),
						plan.activeTaskLimit(),
						plan.conversationLimit(),
						plan.documentStorageBytes()))
				.toList();
		PlanTier.fromKey(currentPlanKey);
		return new PlanCatalogResponse(currentPlanKey, plans);
	}
}
