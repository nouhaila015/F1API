package com.f1.api.service;

import com.f1.api.model.dto.DriverStandingDTO;
import com.f1.api.model.dto.DriverSummaryDTO;
import com.f1.api.model.dto.TeamStandingDTO;
import com.f1.api.model.entity.DriverEntity;
import com.f1.api.model.entity.SessionEntity;
import com.f1.api.model.entity.SessionResultEntity;
import com.f1.api.model.Driver;
import com.f1.api.repository.DriverRepository;
import com.f1.api.repository.SessionRepository;
import com.f1.api.repository.SessionResultRepository;
import com.f1.api.sync.DataSyncService;
import com.f1.api.utils.F1ScoringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final SessionRepository sessionRepository;
    private final DriverRepository driverRepository;
    private final SessionResultRepository sessionResultRepository;
    private final DataSyncService dataSyncService;

    public ScoringService(SessionRepository sessionRepository, DriverRepository driverRepository,
                          SessionResultRepository sessionResultRepository, DataSyncService dataSyncService) {
        this.sessionRepository = sessionRepository;
        this.driverRepository = driverRepository;
        this.sessionResultRepository = sessionResultRepository;
        this.dataSyncService = dataSyncService;
    }

    @Cacheable(value = "driverStandings", condition = "@dataSyncService.isSynced(#year)")
    public List<DriverStandingDTO> getDriverStandings(int year) {
        log.info("Computing driver standings for year {} from DB", year);
        dataSyncService.triggerSyncIfNeeded(year);
        PointsAccumulation acc = computePoints(year);

        List<DriverStandingDTO> standings = acc.pointsByDriver().entrySet().stream()
                .map(entry -> {
                    Driver d = acc.driverInfo().get(entry.getKey());
                    return new DriverStandingDTO(
                            0,
                            d != null ? d.firstName() : "Unknown",
                            d != null ? d.lastName() : String.valueOf(entry.getKey()),
                            d != null ? d.nameAcronym() : "",
                            d != null ? d.headshotURL() : "",
                            entry.getValue());
                })
                .sorted(Comparator.comparingInt(DriverStandingDTO::getPoints).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }

        return standings;
    }

    @Cacheable(value = "teamStandings", condition = "@dataSyncService.isSynced(#year)")
    public List<TeamStandingDTO> getTeamStandings(int year) {
        log.info("Computing team standings for year {} from DB", year);
        dataSyncService.triggerSyncIfNeeded(year);
        PointsAccumulation acc = computePoints(year);

        Map<String, Integer> pointsByTeam = new HashMap<>();
        Map<String, String> colourByTeam = new HashMap<>();
        Map<String, Set<Integer>> driverNumbersByTeam = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : acc.pointsByDriver().entrySet()) {
            Driver driver = acc.driverInfo().get(entry.getKey());
            if (driver == null || driver.teamName() == null) continue;

            String team = driver.teamName();
            pointsByTeam.merge(team, entry.getValue(), Integer::sum);
            colourByTeam.putIfAbsent(team, driver.teamColour());
            driverNumbersByTeam.computeIfAbsent(team, k -> new LinkedHashSet<>()).add(entry.getKey());
        }

        List<TeamStandingDTO> standings = pointsByTeam.entrySet().stream()
                .map(entry -> {
                    String team = entry.getKey();
                    List<DriverSummaryDTO> driverDTOs = driverNumbersByTeam
                            .getOrDefault(team, Set.of()).stream()
                            .map(acc.driverInfo()::get)
                            .filter(Objects::nonNull)
                            .map(d -> new DriverSummaryDTO(d.fullName(), d.nameAcronym(), d.driverNumber()))
                            .toList();
                    return new TeamStandingDTO(entry.getValue(), team, colourByTeam.get(team), driverDTOs, 0);
                })
                .sorted(Comparator.comparingInt(TeamStandingDTO::getPoints).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setPosition(i + 1);
        }

        return standings;
    }

    private PointsAccumulation computePoints(int year) {
        List<SessionEntity> sessions = sessionRepository
                .findByYearAndSessionTypeIn(year, List.of("Race", "Sprint"));

        if (sessions.isEmpty()) {
            return new PointsAccumulation(Map.of(), Map.of());
        }

        List<Integer> sessionKeys = sessions.stream().map(SessionEntity::getSessionKey).toList();

        Map<Integer, List<DriverEntity>> driversBySession = driverRepository
                .findBySessionKeyIn(sessionKeys)
                .stream()
                .collect(Collectors.groupingBy(DriverEntity::getSessionKey));

        Map<Integer, List<SessionResultEntity>> resultsBySession = sessionResultRepository
                .findBySessionKeyIn(sessionKeys)
                .stream()
                .collect(Collectors.groupingBy(SessionResultEntity::getSessionKey));

        Map<Integer, Integer> pointsByDriver = new HashMap<>();
        Map<Integer, Driver> driverInfo = new HashMap<>();

        for (SessionEntity session : sessions) {
            List<DriverEntity> drivers = driversBySession.getOrDefault(session.getSessionKey(), List.of());
            List<SessionResultEntity> results = resultsBySession.getOrDefault(session.getSessionKey(), List.of());

            drivers.forEach(d -> driverInfo.putIfAbsent(d.getDriverNumber(), toDriverModel(d)));

            boolean isSprint = "Sprint".equalsIgnoreCase(session.getSessionType());
            int fastestLapDriver = resolveFastestLapDriver(isSprint, session);

            results.forEach(result ->
                    pointsByDriver.merge(result.getDriverNumber(),
                            calculateResultPoints(result, isSprint, fastestLapDriver),
                            Integer::sum));
        }

        return new PointsAccumulation(pointsByDriver, driverInfo);
    }

    private int resolveFastestLapDriver(boolean isSprint, SessionEntity session) {
        if (isSprint) return -1;
        if (session.getFastestLapDriverNumber() != null) return session.getFastestLapDriverNumber();
        return -1;
    }

    private int calculateResultPoints(SessionResultEntity result, boolean isSprint, int fastestLapDriver) {
        boolean retired = result.isDnf() || result.isDns() || result.isDsq();
        int position = result.getPosition() != null ? result.getPosition() : 0;
        if (retired) position = 0;
        if (isSprint) {
            return F1ScoringUtils.calculateSprintPoints(position);
        }
        return F1ScoringUtils.calculatePoints(position, result.getDriverNumber() == fastestLapDriver);
    }

    private Driver toDriverModel(DriverEntity e) {
        return new Driver(e.getCountryCode(), e.getDriverNumber(), e.getFirstName(),
                e.getFullName(), e.getLastName(), e.getHeadshotUrl(),
                e.getNameAcronym(), e.getTeamColour(), e.getTeamName());
    }

    private record PointsAccumulation(Map<Integer, Integer> pointsByDriver, Map<Integer, Driver> driverInfo) {}
}
