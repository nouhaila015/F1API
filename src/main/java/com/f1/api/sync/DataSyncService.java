package com.f1.api.sync;

import com.f1.api.client.OpenF1Client;
import com.f1.api.exception.OpenF1Exception;
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

    // Self-injection so @Async and @Transactional are proxied correctly
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
            long expected = openF1Client.getSessions(year).stream()
                    .filter(s -> ("Race".equalsIgnoreCase(s.sessionType()) || "Sprint".equalsIgnoreCase(s.sessionType()))
                            && s.dateEnd() != null && s.dateEnd().isBefore(OffsetDateTime.now()))
                    .count();
            long actual = sessionRepository.countByYear(year);
            if (actual >= expected) {
                syncedYears.add(year);
                log.info("Sync complete for year {} ({} sessions)", year, actual);
            } else {
                log.warn("Partial sync for year {}: {}/{}. Will retry on next request.", year, actual, expected);
            }
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
                .filter(s -> "Race".equalsIgnoreCase(s.sessionType()) || "Sprint".equalsIgnoreCase(s.sessionType()))
                .filter(s -> s.dateEnd() != null && s.dateEnd().isBefore(now))
                .filter(s -> !sessionRepository.existsById(s.sessionKey()))
                .forEach(session -> {
                    try {
                        self.syncSession(session);
                        log.info("Synced session {} ({} - {})", session.sessionKey(), session.countryName(), session.sessionType());
                    } catch (OpenF1Exception e) {
                        if (e.getStatusCode() == 429) {
                            log.warn("Rate limited — aborting sync for year {}", year);
                            throw e;
                        }
                        log.error("Failed to sync session {}: {}", session.sessionKey(), e.getMessage());
                    }
                    sleepMillis(2000);
                });
    }

    @Transactional
    public void syncSession(Session session) {
        int sessionKey = session.sessionKey();
        boolean isSprint = "Sprint".equalsIgnoreCase(session.sessionType());

        List<Driver> drivers = openF1Client.getDrivers(sessionKey);
        sleepMillis(2000);
        List<SessionResult> results = openF1Client.getSessionResults(sessionKey);
        sleepMillis(2000);

        Integer fastestLapDriverNumber = null;
        if (!isSprint) {
            var laps = openF1Client.getLaps(sessionKey);
            int found = F1ScoringUtils.findFastestLapDriverNumber(laps);
            fastestLapDriverNumber = found >= 0 ? found : null;
            sleepMillis(2000);
        }

        sessionRepository.save(new SessionEntity(
                sessionKey, session.circuitShortName(), session.countryCode(), session.countryName(),
                session.dateEnd(), session.dateStart(), session.sessionName(), session.sessionType(),
                session.year(), fastestLapDriverNumber));

        driverRepository.saveAll(drivers.stream()
                .map(d -> new DriverEntity(sessionKey, d.driverNumber(), d.countryCode(),
                        d.firstName(), d.fullName(), d.lastName(), d.headshotURL(),
                        d.nameAcronym(), d.teamColour(), d.teamName()))
                .toList());

        sessionResultRepository.saveAll(results.stream()
                .map(r -> new SessionResultEntity(sessionKey, r.driverNumber(), r.dnf(), r.dns(), r.dsq(),
                        r.duration(), r.gapToLeader(), r.numberOfLaps(), r.position()))
                .toList());
    }

    private void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
