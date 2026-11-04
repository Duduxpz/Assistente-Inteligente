package com.aetherai.admin;

import com.aetherai.admin.service.AdminService;
import com.aetherai.common.exception.ApiException;
import com.aetherai.usage.dto.UsageTotals;
import com.aetherai.usage.repository.UsageRepository;
import com.aetherai.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServiceTest {

	@Test
	void reportsPeriodUsageAndUserCountsByPlan() {
		UserRepository users = mock(UserRepository.class);
		UsageRepository usage = mock(UsageRepository.class);
		when(users.countByPlanKey(anyString())).thenAnswer(invocation ->
				Map.of("free", 8L, "plus", 3L, "ultra", 1L).get(invocation.getArgument(0)));
		when(usage.countByOperationAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
				anyString(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any())).thenReturn(9L);
		when(usage.totalsBetween(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(new UsageTotals(20L, 30L, 9L));

		Instant from = Instant.parse("2026-01-01T00:00:00Z");
		Instant to = Instant.parse("2026-01-31T00:00:00Z");
		var result = new AdminService(users, usage).metrics(from, to);

		assertEquals(9L, result.messages());
		assertEquals(new UsageTotals(20L, 30L, 9L), result.usage());
		assertEquals(Map.of("free", 8L, "plus", 3L, "ultra", 1L), result.usersByPlan());
	}

	@Test
	void rejectsInvalidOrExcessivePeriods() {
		AdminService service = new AdminService(mock(UserRepository.class), mock(UsageRepository.class));

		assertThrows(ApiException.class, () -> service.metrics(
				Instant.parse("2026-02-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z")));
		assertThrows(ApiException.class, () -> service.metrics(
				Instant.parse("2024-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z")));
	}
}
