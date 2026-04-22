package com.aiassistant.persistence.repository;

import com.aiassistant.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {
}
