package com.aetherai.task.repository;

import com.aetherai.task.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

	List<TaskEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<TaskEntity> findByIdAndUserId(UUID id, UUID userId);
}
