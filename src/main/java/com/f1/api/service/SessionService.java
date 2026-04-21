package com.f1.api.service;

import com.f1.api.model.entity.SessionEntity;
import com.f1.api.model.Session;
import com.f1.api.repository.SessionRepository;
import com.f1.api.sync.DataSyncService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final DataSyncService dataSyncService;

    public SessionService(SessionRepository sessionRepository, DataSyncService dataSyncService) {
        this.sessionRepository = sessionRepository;
        this.dataSyncService = dataSyncService;
    }

    @Cacheable(value = "seasonRaces", condition = "@dataSyncService.isSynced(#year)")
    public List<Session> getSeasonRaces(int year) {
        dataSyncService.triggerSyncIfNeeded(year);
        return sessionRepository
                .findByYearAndSessionTypeOrderByDateStartAsc(year, "Race")
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Cacheable(value = "pointsSessions", condition = "@dataSyncService.isSynced(#year)")
    public List<Session> getPointsSessions(int year) {
        dataSyncService.triggerSyncIfNeeded(year);
        return sessionRepository
                .findByYearAndSessionTypeIn(year, List.of("Race", "Sprint"))
                .stream()
                .map(this::toModel)
                .toList();
    }

    private Session toModel(SessionEntity e) {
        return new Session(
                e.getCircuitShortName(),
                e.getCountryCode(),
                e.getCountryName(),
                e.getDateEnd(),
                e.getDateStart(),
                e.getSessionKey(),
                e.getSessionName(),
                e.getSessionType(),
                e.getYear()
        );
    }
}
