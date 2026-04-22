package com.aiassistant.persistence.repository;

import com.aiassistant.persistence.entity.DeviceInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceInfoRepository extends JpaRepository<DeviceInfoEntity, String> {
    List<DeviceInfoEntity> findByActiveTrueOrderByNameAscDeviceIdAsc();
}
