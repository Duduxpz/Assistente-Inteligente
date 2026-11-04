package com.aetherai.admin.service;

import com.aetherai.admin.dto.AdminUserResponse;
import com.aetherai.admin.dto.AdminMetricsResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.plan.PlanTier;
import com.aetherai.usage.repository.UsageRepository;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final UsageRepository usageRepository;

	public AdminService(UserRepository userRepository, UsageRepository usageRepository) {
		this.userRepository = userRepository;
		this.usageRepository = usageRepository;
	}

	@Transactional(readOnly = true)
	public Page<AdminUserResponse> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(AdminUserResponse::from);
	}

	@Transactional(readOnly = true)
	public AdminMetricsResponse metrics(Instant from, Instant to) {
		if (!from.isBefore(to) || Duration.between(from, to).compareTo(Duration.ofDays(366)) > 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The metrics period must be valid and no longer than 366 days.");
		}
		Map<String, Long> usersByPlan = new LinkedHashMap<>();
		for (PlanTier plan : PlanTier.values()) {
			usersByPlan.put(plan.key(), userRepository.countByPlanKey(plan.key()));
		}
		long messages = usageRepository.countByOperationAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
				"chat.completion",
				from,
				to);
		return new AdminMetricsResponse(
				from,
				to,
				messages,
				usageRepository.totalsBetween(from, to),
				usersByPlan);
	}

	@Transactional
	public AdminUserResponse setUserEnabled(UUID userId, boolean enabled) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		user.setEnabled(enabled);
		return AdminUserResponse.from(userRepository.save(user));
	}
}
