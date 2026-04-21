package com.f1.api.repository;

import com.f1.api.model.entity.SessionResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionResultRepository extends JpaRepository<SessionResultEntity, Long> {

    List<SessionResultEntity> findBySessionKey(int sessionKey);

    List<SessionResultEntity> findBySessionKeyIn(List<Integer> sessionKeys);
}
