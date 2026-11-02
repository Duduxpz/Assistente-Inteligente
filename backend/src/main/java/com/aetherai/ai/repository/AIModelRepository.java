package com.aetherai.ai.repository;

import com.aetherai.ai.entity.AIModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIModelRepository extends JpaRepository<AIModelEntity, Long> {

	Optional<AIModelEntity> findByProviderAndModelKey(String provider, String modelKey);
}
