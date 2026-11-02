package com.aetherai.plan.dto;

import java.util.List;

public record PlanCatalogResponse(String currentPlanKey, List<PlanDetails> plans) {
}
