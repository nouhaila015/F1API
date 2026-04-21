package com.f1.api.repository;

import com.f1.api.model.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<DriverEntity, Long> {

    List<DriverEntity> findBySessionKey(int sessionKey);

    List<DriverEntity> findBySessionKeyIn(List<Integer> sessionKeys);

    Optional<DriverEntity> findBySessionKeyAndDriverNumber(int sessionKey, int driverNumber);
}
