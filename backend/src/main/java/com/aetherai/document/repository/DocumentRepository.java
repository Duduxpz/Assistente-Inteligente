package com.aetherai.document.repository;

import com.aetherai.document.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

	List<DocumentEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<DocumentEntity> findByIdAndUserId(UUID id, UUID userId);
}
