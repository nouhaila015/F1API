package com.f1.api.service;

import com.f1.api.client.OpenF1Client;
import com.f1.api.dto.DriverStandingDTO;
import com.f1.api.dto.DriverSummaryDTO;
import com.f1.api.dto.TeamStandingDTO;
import com.f1.api.model.Driver;
import com.f1.api.model.Lap;
import com.f1.api.model.Session;
import com.f1.api.model.SessionResult;
import com.f1.api.utils.F1ScoringUtils;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private final OpenF1Client openF1Client;
    private final SessionService sessionService;

    public ScoringService(OpenF1Client openF1Client, SessionService sessionService) {
        this.openF1Client = openF1Client;
        this.sessionService = sessionService;
    }

    @Cacheable("driverStandings")
    public List<DriverStandingDTO> getDriverStandings(int year) {
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

    @Cacheable("teamStandings")
    public List<TeamStandingDTO> getTeamStandings(int year) {
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

    // Fetches all race sessions for the year, computes each driver's total points,
    // and collects driver metadata — shared by both standing calculations.
    private PointsAccumulation computePoints(int year) {
        List<SessionData> sessionDataList = fetchAllSessionData(sessionService.getPointsSessions(year));

        Map<Integer, Integer> pointsByDriver = new HashMap<>();
        Map<Integer, Driver> driverInfo = new HashMap<>();

        for (SessionData data : sessionDataList) {
            data.drivers().forEach(d -> driverInfo.putIfAbsent(d.driverNumber(), d));
            boolean isSprint = "Sprint".equalsIgnoreCase(data.sessionType());
            int fastestLapDriver = isSprint ? -1 : F1ScoringUtils.findFastestLapDriverNumber(data.laps());

            for (SessionResult result : data.results()) {
                int position = isRetired(result) ? 0 : result.position();
                int points = isSprint
                        ? F1ScoringUtils.calculateSprintPoints(position)
                        : F1ScoringUtils.calculatePoints(position, result.driverNumber() == fastestLapDriver);
                pointsByDriver.merge(result.driverNumber(), points, Integer::sum);
            }
        }

        return new PointsAccumulation(pointsByDriver, driverInfo);
    }

    private List<SessionData> fetchAllSessionData(List<Session> sessions) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<SessionData>> futures = sessions.stream()
                    .map(session -> fetchSessionAsync(session, executor))
                    .toList();
            return futures.stream().map(CompletableFuture::join).toList();
        }
    }

    private CompletableFuture<SessionData> fetchSessionAsync(Session session, Executor executor) {
        int sessionKey = session.sessionKey();
        var driversFuture = CompletableFuture.supplyAsync(() -> openF1Client.getDrivers(sessionKey), executor);
        var resultsFuture = CompletableFuture.supplyAsync(() -> openF1Client.getSessionResults(sessionKey), executor);
        var lapsFuture    = CompletableFuture.supplyAsync(() -> openF1Client.getLaps(sessionKey), executor);
        return CompletableFuture.allOf(driversFuture, resultsFuture, lapsFuture)
                .thenApply(v -> new SessionData(session.sessionType(), driversFuture.join(), resultsFuture.join(), lapsFuture.join()));
    }

    private boolean isRetired(SessionResult result) {
        return result.dnf() || result.dns() || result.dsq();
    }

    private record SessionData(String sessionType, List<Driver> drivers, List<SessionResult> results, List<Lap> laps) {}

    private record PointsAccumulation(Map<Integer, Integer> pointsByDriver, Map<Integer, Driver> driverInfo) {}
}
