package com.aetherai.usage.repository;

import com.aetherai.usage.entity.UsageLogEntity;
import com.aetherai.usage.dto.UsageTotals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface UsageRepository extends JpaRepository<UsageLogEntity, Long> {

	List<UsageLogEntity> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);

	long countByUserIdAndOperationAndCreatedAtGreaterThanEqual(UUID userId, String operation, Instant createdAt);

	@Query("""
			select new com.aetherai.usage.dto.UsageTotals(
				coalesce(sum(u.inputTokens), 0),
				coalesce(sum(u.outputTokens), 0),
				count(u))
			from UsageLogEntity u
			where u.userId = :userId
			""")
	UsageTotals totalsForUser(@Param("userId") UUID userId);

	@Query("""
			select new com.aetherai.usage.dto.UsageTotals(
				coalesce(sum(u.inputTokens), 0),
				coalesce(sum(u.outputTokens), 0),
				count(u))
			from UsageLogEntity u
			""")
	UsageTotals totalsForAllUsers();

	@Query("""
			select new com.aetherai.usage.dto.UsageTotals(
				coalesce(sum(u.inputTokens), 0),
				coalesce(sum(u.outputTokens), 0),
				count(u))
			from UsageLogEntity u
			where u.createdAt >= :from and u.createdAt < :to
			""")
	UsageTotals totalsBetween(@Param("from") Instant from, @Param("to") Instant to);

	long countByOperationAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			String operation,
			Instant from,
			Instant to);
}
