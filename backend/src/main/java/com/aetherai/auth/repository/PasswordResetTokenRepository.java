package com.aetherai.auth.repository;

import com.aetherai.auth.service.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select token from PasswordResetTokenEntity token
			where token.tokenHash = :tokenHash
				and token.usedAt is null
				and token.expiresAt > :now
			""")
	Optional<PasswordResetTokenEntity> findValidTokenForUpdate(
			@Param("tokenHash") String tokenHash,
			@Param("now") Instant now);

	Optional<PasswordResetTokenEntity> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

	void deleteAllByUserId(UUID userId);
}
