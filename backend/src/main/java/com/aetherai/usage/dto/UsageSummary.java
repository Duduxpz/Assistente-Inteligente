package com.aetherai.usage.dto;

import java.util.List;

public record UsageSummary(UsageTotals totals, List<UsageEntry> recentActivity) {
}
