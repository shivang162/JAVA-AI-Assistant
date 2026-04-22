package com.aiassistant.persistence.repository;

import com.aiassistant.persistence.entity.GroupChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessageEntity, Long> {
    List<GroupChatMessageEntity> findByGroupIdOrderBySentAtAsc(String groupId);
}
