package com.aetherai.message.repository;

import com.aetherai.message.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

	List<MessageEntity> findAllByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);
}
