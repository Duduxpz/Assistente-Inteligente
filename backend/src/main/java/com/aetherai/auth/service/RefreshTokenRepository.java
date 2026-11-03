package com.aetherai.auth.service;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select token from RefreshTokenEntity token where token.tokenHash = :tokenHash")
	Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	@Modifying
	@Query("update RefreshTokenEntity token set token.revokedAt = :revokedAt where token.userId = :userId and token.revokedAt is null")
	int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

	@Modifying
	@Query("update RefreshTokenEntity token set token.revokedAt = :revokedAt where token.familyId = :familyId and token.revokedAt is null")
	int revokeAllByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);
}
