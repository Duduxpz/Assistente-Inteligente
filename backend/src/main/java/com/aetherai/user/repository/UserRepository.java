package com.aetherai.user.repository;

import com.aetherai.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	@EntityGraph(attributePaths = "roles")
	Optional<UserEntity> findByEmail(String email);

	@EntityGraph(attributePaths = "roles")
	Optional<UserEntity> findById(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select user from UserEntity user where user.id = :id")
	Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

	boolean existsByEmail(String email);
}
