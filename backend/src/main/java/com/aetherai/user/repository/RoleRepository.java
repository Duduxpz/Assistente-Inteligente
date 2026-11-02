package com.aetherai.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aetherai.user.entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

	Optional<RoleEntity> findByName(String name);
}
