package com.aiassistant.persistence.repository;

import com.aiassistant.persistence.entity.GroupChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupChatRepository extends JpaRepository<GroupChatEntity, String> {
}
