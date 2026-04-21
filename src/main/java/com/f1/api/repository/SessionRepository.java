package com.f1.api.repository;

import com.f1.api.model.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, Integer> {

    List<SessionEntity> findByYearAndSessionTypeOrderByDateStartAsc(int year, String sessionType);

    List<SessionEntity> findByYearAndSessionTypeIn(int year, List<String> sessionTypes);

    List<SessionEntity> findByYear(int year);

    boolean existsByYear(int year);
}
