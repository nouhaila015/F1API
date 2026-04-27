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

    /**
     * Returns all Grand Prix sessions for the given championship year, ordered by date.
     * Triggers a background data sync if the year has not been synced yet.
     * Results are cached under {@code seasonRaces} once the sync is complete.
     *
     * @param year championship year (e.g. 2024)
     * @return race sessions sorted by start date, never {@code null}
     */
    @Cacheable(value = "seasonRaces", condition = "@dataSyncService.isSynced(#year)")
    public List<Session> getSeasonRaces(int year) {
        dataSyncService.triggerSyncIfNeeded(year);
        return sessionRepository
                .findByYearAndSessionTypeOrderByDateStartAsc(year, "Race")
                .stream()
                .map(this::toModel)
                .toList();
    }

    /**
     * Returns all points-scoring sessions (Grand Prix + Sprint) for the given year, ordered by date.
     * Used by the scoring engine to calculate standings.
     * Results are cached under {@code pointsSessions} once the sync is complete.
     *
     * @param year championship year (e.g. 2024)
     * @return race and sprint sessions sorted by start date, never {@code null}
     */
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
