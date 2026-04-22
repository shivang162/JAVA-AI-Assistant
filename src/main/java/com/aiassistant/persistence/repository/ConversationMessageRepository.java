package com.aiassistant.persistence.repository;

import com.aiassistant.persistence.entity.ConversationMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, Long> {
    List<ConversationMessageEntity> findByOrderByCreatedAtAsc(Pageable pageable);

    List<ConversationMessageEntity> findByOrderByCreatedAtDesc(Pageable pageable);
}
