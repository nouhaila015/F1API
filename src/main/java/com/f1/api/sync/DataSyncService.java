package com.f1.api.sync;

import com.f1.api.client.OpenF1Client;
import com.f1.api.model.entity.DriverEntity;
import com.f1.api.model.entity.SessionEntity;
import com.f1.api.model.entity.SessionResultEntity;
import com.f1.api.model.Driver;
import com.f1.api.model.Session;
import com.f1.api.model.SessionResult;
import com.f1.api.repository.DriverRepository;
import com.f1.api.repository.SessionRepository;
import com.f1.api.repository.SessionResultRepository;
import com.f1.api.utils.F1ScoringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final OpenF1Client openF1Client;
    private final SessionRepository sessionRepository;
    private final DriverRepository driverRepository;
    private final SessionResultRepository sessionResultRepository;

    // Self-injection to allow @Async and @Transactional to be proxied correctly
    private final DataSyncService self;

    private final Set<Integer> syncedYears = ConcurrentHashMap.newKeySet();
    private final Set<Integer> syncingYears = ConcurrentHashMap.newKeySet();

    public DataSyncService(OpenF1Client openF1Client, SessionRepository sessionRepository,
                           DriverRepository driverRepository, SessionResultRepository sessionResultRepository,
                           @Lazy DataSyncService self) {
        this.openF1Client = openF1Client;
        this.sessionRepository = sessionRepository;
        this.driverRepository = driverRepository;
        this.sessionResultRepository = sessionResultRepository;
        this.self = self;
    }

    public boolean isSynced(int year) {
        return syncedYears.contains(year);
    }

    /**
     * Triggers an async sync for the given year if not already synced or syncing.
     * Returns immediately — callers should query the DB and return whatever is available.
     */
    public void triggerSyncIfNeeded(int year) {
        if (syncedYears.contains(year)) return;
        if (syncingYears.add(year)) {
            self.doSyncYear(year);
        }
    }

    @Async
    public void doSyncYear(int year) {
        try {
            syncYear(year);
            syncedYears.add(year);
            log.info("Sync complete for year {}", year);
        } finally {
            syncingYears.remove(year);
        }
    }

    public void syncYear(int year) {
        List<Session> sessions;
        try {
            sessions = openF1Client.getSessions(year);
        } catch (Exception e) {
            log.error("Failed to fetch sessions for year {}: {}", year, e.getMessage());
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        sessions.stream()
                .filter(session -> session.dateEnd() != null && session.dateEnd().isBefore(now))
                .filter(session -> !sessionRepository.existsById(session.sessionKey()))
                .forEach(session -> {
                    try {
                        self.syncSession(session);
                        log.info("Synced session {} ({} - {})", session.sessionKey(), session.countryName(), session.sessionType());
                    } catch (Exception e) {
                        log.error("Failed to sync session {} ({}): {}", session.sessionKey(), session.sessionName(), e.getMessage());
                    }
                    sleepMillis(500);
                });
    }

    @Transactional
    public void syncSession(Session session) {
        int sessionKey = session.sessionKey();
        boolean isSprint = "Sprint".equalsIgnoreCase(session.sessionType());

        List<Driver> drivers = openF1Client.getDrivers(sessionKey);
        sleepMillis(400);
        List<SessionResult> results = openF1Client.getSessionResults(sessionKey);
        sleepMillis(400);

        Integer fastestLapDriverNumber = null;
        if (!isSprint) {
            var laps = openF1Client.getLaps(sessionKey);
            int found = F1ScoringUtils.findFastestLapDriverNumber(laps);
            fastestLapDriverNumber = found >= 0 ? found : null;
            sleepMillis(400);
        }

        SessionEntity sessionEntity = new SessionEntity(
                sessionKey,
                session.circuitShortName(),
                session.countryCode(),
                session.countryName(),
                session.dateEnd(),
                session.dateStart(),
                session.sessionName(),
                session.sessionType(),
                session.year(),
                fastestLapDriverNumber
        );
        sessionRepository.save(sessionEntity);

        List<DriverEntity> driverEntities = drivers.stream()
                .map(d -> new DriverEntity(sessionKey, d.driverNumber(), d.countryCode(),
                        d.firstName(), d.fullName(), d.lastName(), d.headshotURL(),
                        d.nameAcronym(), d.teamColour(), d.teamName()))
                .toList();
        driverRepository.saveAll(driverEntities);

        List<SessionResultEntity> resultEntities = results.stream()
                .map(r -> new SessionResultEntity(sessionKey, r.driverNumber(), r.dnf(), r.dns(), r.dsq(),
                        r.duration(), r.gapToLeader(), r.numberOfLaps(), r.position()))
                .toList();
        sessionResultRepository.saveAll(resultEntities);
    }

    private void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
