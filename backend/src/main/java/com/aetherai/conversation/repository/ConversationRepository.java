package com.aetherai.conversation.repository;

import com.aetherai.conversation.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

	List<ConversationEntity> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

	Optional<ConversationEntity> findByIdAndUserId(UUID id, UUID userId);
}
