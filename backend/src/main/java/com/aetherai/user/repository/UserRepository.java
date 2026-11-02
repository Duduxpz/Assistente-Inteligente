package com.aetherai.user.repository;

import com.aetherai.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	@EntityGraph(attributePaths = "roles")
	Optional<UserEntity> findByEmail(String email);

	@EntityGraph(attributePaths = "roles")
	Optional<UserEntity> findById(UUID id);

	boolean existsByEmail(String email);
}
