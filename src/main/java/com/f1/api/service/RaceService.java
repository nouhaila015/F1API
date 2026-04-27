package com.f1.api.service;

import com.f1.api.model.dto.DriverStatsDTO;
import com.f1.api.model.dto.RaceResultDTO;
import com.f1.api.model.entity.DriverEntity;
import com.f1.api.model.entity.SessionEntity;
import com.f1.api.model.entity.SessionResultEntity;
import com.f1.api.repository.DriverRepository;
import com.f1.api.repository.SessionRepository;
import com.f1.api.repository.SessionResultRepository;
import com.f1.api.sync.DataSyncService;
import com.f1.api.utils.F1ScoringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RaceService {

    private static final Logger log = LoggerFactory.getLogger(RaceService.class);
    private static final String UNKNOWN = "Unknown";

    private final SessionRepository sessionRepository;
    private final DriverRepository driverRepository;
    private final SessionResultRepository sessionResultRepository;
    private final DataSyncService dataSyncService;

    public RaceService(SessionRepository sessionRepository, DriverRepository driverRepository,
                       SessionResultRepository sessionResultRepository, DataSyncService dataSyncService) {
        this.sessionRepository = sessionRepository;
        this.driverRepository = driverRepository;
        this.sessionResultRepository = sessionResultRepository;
        this.dataSyncService = dataSyncService;
    }

    /**
     * Returns the classified results for a single race or sprint session.
     * Finishers are sorted by position; DNF/DNS/DSQ entries are sorted to the end.
     * Results are cached under {@code raceResults} keyed by session.
     *
     * @param sessionKey OpenF1 session key
     * @return ordered list of race results, never {@code null}
     */
    @Cacheable("raceResults")
    public List<RaceResultDTO> getRaceResults(int sessionKey) {
        log.info("Loading race results for session {} from DB", sessionKey);

        Map<Integer, DriverEntity> driverMap = driverRepository.findBySessionKey(sessionKey)
                .stream()
                .collect(Collectors.toMap(DriverEntity::getDriverNumber, d -> d, (a, b) -> a));

        int fastestLapDriver = sessionRepository.findById(sessionKey)
                .map(s -> s.getFastestLapDriverNumber() != null ? s.getFastestLapDriverNumber() : -1)
                .orElse(-1);

        return sessionResultRepository.findBySessionKey(sessionKey)
                .stream()
                .map(result -> {
                    DriverEntity d = driverMap.get(result.getDriverNumber());
                    int position = result.getPosition() != null ? result.getPosition() : 0;
                    return new RaceResultDTO(
                            position,
                            result.getDriverNumber(),
                            d != null ? d.getFullName() : UNKNOWN,
                            d != null ? d.getNameAcronym() : "",
                            d != null ? d.getTeamName() : "",
                            d != null ? d.getTeamColour() : "",
                            result.getGapToLeader(),
                            result.getDriverNumber() == fastestLapDriver,
                            result.isDnf(),
                            result.isDns(),
                            result.isDsq());
                })
                .sorted(Comparator.comparingInt(r -> r.dnf() || r.dns() || r.dsq() ? Integer.MAX_VALUE : r.position()))
                .toList();
    }

    /**
     * Returns aggregated season statistics for a single driver.
     * Covers Grand Prix results only for wins, podiums, and best finish;
     * sprint points are included in the total points tally.
     * Results are cached under {@code driverStats} once the year is fully synced.
     *
     * @param driverNumber official F1 car number (e.g. 1 for Verstappen)
     * @param year         championship year (e.g. 2024)
     * @return driver stats DTO; returns zeroed DTO if the driver has no results
     */
    @Cacheable(value = "driverStats", condition = "@dataSyncService.isSynced(#year)")
    public DriverStatsDTO getDriverStats(int driverNumber, int year) {
        log.info("Loading stats for driver {} in year {} from DB", driverNumber, year);
        dataSyncService.triggerSyncIfNeeded(year);

        List<SessionEntity> raceSessions = sessionRepository
                .findByYearAndSessionTypeOrderByDateStartAsc(year, "Race");

        if (raceSessions.isEmpty()) {
            return new DriverStatsDTO(driverNumber, UNKNOWN, "", "", 0, 0, 0, 0, 0, null);
        }

        List<Integer> sessionKeys = raceSessions.stream().map(SessionEntity::getSessionKey).toList();
        Map<Integer, SessionResultEntity> resultBySession = sessionResultRepository
                .findBySessionKeyIn(sessionKeys)
                .stream()
                .filter(r -> r.getDriverNumber() == driverNumber)
                .collect(Collectors.toMap(SessionResultEntity::getSessionKey, r -> r, (a, b) -> a));

        if (resultBySession.isEmpty()) {
            return new DriverStatsDTO(driverNumber, UNKNOWN, "", "", 0, 0, 0, 0, 0, null);
        }

        DriverEntity driverEntity = raceSessions.stream()
                .filter(s -> resultBySession.containsKey(s.getSessionKey()))
                .findFirst()
                .flatMap(s -> driverRepository.findBySessionKeyAndDriverNumber(s.getSessionKey(), driverNumber))
                .orElse(null);

        Map<Integer, Integer> fastestLapBySession = raceSessions.stream()
                .filter(s -> s.getFastestLapDriverNumber() != null)
                .collect(Collectors.toMap(SessionEntity::getSessionKey, SessionEntity::getFastestLapDriverNumber));

        DriverStatsAccumulator stats = new DriverStatsAccumulator();
        raceSessions.stream()
                .filter(session -> resultBySession.containsKey(session.getSessionKey()))
                .forEach(session -> accumulate(stats, session, resultBySession.get(session.getSessionKey()), fastestLapBySession));

        return new DriverStatsDTO(
                driverNumber,
                driverEntity != null ? driverEntity.getFullName() : UNKNOWN,
                driverEntity != null ? driverEntity.getNameAcronym() : "",
                driverEntity != null ? driverEntity.getTeamName() : "",
                stats.totalPoints,
                resultBySession.size(),
                stats.wins,
                stats.podiums,
                stats.dnfs,
                stats.bestFinish);
    }

    private void accumulate(DriverStatsAccumulator stats, SessionEntity session,
                            SessionResultEntity result, Map<Integer, Integer> fastestLapBySession) {
        if (result.isDnf() || result.isDns() || result.isDsq()) {
            if (result.isDnf()) stats.dnfs++;
            return;
        }

        int position = result.getPosition() != null ? result.getPosition() : 0;
        boolean isSprint = "Sprint".equalsIgnoreCase(session.getSessionName());

        // Points count for both GP and Sprint
        if (isSprint) {
            stats.totalPoints += F1ScoringUtils.calculateSprintPoints(position);
        } else {
            int fastestLapDriver = fastestLapBySession.getOrDefault(session.getSessionKey(), -1);
            stats.totalPoints += F1ScoringUtils.calculatePoints(position, result.getDriverNumber() == fastestLapDriver);
        }

        // Wins, podiums, and bestFinish only count Grand Prix (not Sprints)
        if (!isSprint) {
            if (position == 1) stats.wins++;
            if (position <= 3) stats.podiums++;
            if (stats.bestFinish == null || position < stats.bestFinish) stats.bestFinish = position;
        }
    }

    private static class DriverStatsAccumulator {
        int totalPoints;
        int wins;
        int podiums;
        int dnfs;
        Integer bestFinish;
    }
}
