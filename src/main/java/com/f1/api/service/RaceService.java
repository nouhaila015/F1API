package com.f1.api.service;

import com.f1.api.client.OpenF1Client;
import com.f1.api.dto.DriverStatsDTO;
import com.f1.api.dto.RaceResultDTO;
import com.f1.api.model.Driver;
import com.f1.api.model.Session;
import com.f1.api.model.SessionResult;
import com.f1.api.utils.F1ScoringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class RaceService {

    private final OpenF1Client openF1Client;
    private final SessionService sessionService;

    public RaceService(OpenF1Client openF1Client, SessionService sessionService) {
        this.openF1Client = openF1Client;
        this.sessionService = sessionService;
    }

    @Cacheable("raceResults")
    public List<RaceResultDTO> getRaceResults(int sessionKey) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var driversFuture = CompletableFuture.supplyAsync(() -> openF1Client.getDrivers(sessionKey), executor);
            var resultsFuture = CompletableFuture.supplyAsync(() -> openF1Client.getSessionResults(sessionKey), executor);
            var lapsFuture    = CompletableFuture.supplyAsync(() -> openF1Client.getLaps(sessionKey), executor);

            CompletableFuture.allOf(driversFuture, resultsFuture, lapsFuture).join();

            Map<Integer, Driver> driverMap = driversFuture.join().stream()
                    .collect(Collectors.toMap(Driver::driverNumber, d -> d, (a, b) -> a));
            int fastestLapDriver = F1ScoringUtils.findFastestLapDriverNumber(lapsFuture.join());

            return resultsFuture.join().stream()
                    .map(result -> {
                        Driver d = driverMap.get(result.driverNumber());
                        return new RaceResultDTO(
                                result.position(),
                                result.driverNumber(),
                                d != null ? d.fullName() : "Unknown",
                                d != null ? d.nameAcronym() : "",
                                d != null ? d.teamName() : "",
                                d != null ? d.teamColour() : "",
                                result.gapToLeader(),
                                result.driverNumber() == fastestLapDriver,
                                result.dnf(),
                                result.dns(),
                                result.dsq());
                    })
                    .sorted(Comparator.comparingInt(r -> r.dnf() || r.dns() || r.dsq() ? Integer.MAX_VALUE : r.position()))
                    .toList();
        }
    }

    @Cacheable("driverStats")
    public DriverStatsDTO getDriverStats(int driverNumber, int year) {
        List<Session> races = sessionService.getSeasonRaces(year);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<List<SessionResult>>> resultFutures = races.stream()
                    .map(race -> CompletableFuture.supplyAsync(() -> openF1Client.getSessionResults(race.sessionKey()), executor))
                    .toList();

            CompletableFuture.allOf(resultFutures.toArray(new CompletableFuture[0])).join();

            record RaceEntry(Session race, SessionResult result) {}
            List<RaceEntry> driverRaces = new ArrayList<>();
            for (int i = 0; i < races.size(); i++) {
                final int idx = i;
                resultFutures.get(idx).join().stream()
                        .filter(r -> r.driverNumber() == driverNumber)
                        .findFirst()
                        .ifPresent(r -> driverRaces.add(new RaceEntry(races.get(idx), r)));
            }

            if (driverRaces.isEmpty()) {
                return new DriverStatsDTO(driverNumber, "Unknown", "", "", 0, 0, 0, 0, 0, null);
            }

            int firstSessionKey = driverRaces.getFirst().race().sessionKey();
            var driverInfoFuture = CompletableFuture.supplyAsync(
                    () -> openF1Client.getDrivers(firstSessionKey).stream()
                            .filter(d -> d.driverNumber() == driverNumber)
                            .findFirst().orElse(null),
                    executor);

            List<RaceEntry> finishedRaces = driverRaces.stream()
                    .filter(e -> !e.result().dnf() && !e.result().dns() && !e.result().dsq())
                    .toList();

            Map<Integer, CompletableFuture<Integer>> fastestLapFutures = finishedRaces.stream()
                    .collect(Collectors.toMap(
                            e -> e.race().sessionKey(),
                            e -> CompletableFuture.supplyAsync(
                                    () -> F1ScoringUtils.findFastestLapDriverNumber(openF1Client.getLaps(e.race().sessionKey())),
                                    executor)));

            CompletableFuture.allOf(fastestLapFutures.values().toArray(new CompletableFuture[0])).join();

            int totalPoints = 0, wins = 0, podiums = 0, dnfs = 0;
            Integer bestFinish = null;

            for (RaceEntry entry : driverRaces) {
                SessionResult result = entry.result();
                if (result.dnf() || result.dns() || result.dsq()) {
                    if (result.dnf()) dnfs++;
                    continue;
                }
                int fastestLap = fastestLapFutures.get(entry.race().sessionKey()).join();
                totalPoints += F1ScoringUtils.calculatePoints(result.position(), result.driverNumber() == fastestLap);
                if (result.position() == 1) wins++;
                if (result.position() <= 3) podiums++;
                if (bestFinish == null || result.position() < bestFinish) bestFinish = result.position();
            }

            Driver driver = driverInfoFuture.join();
            return new DriverStatsDTO(
                    driverNumber,
                    driver != null ? driver.fullName() : "Unknown",
                    driver != null ? driver.nameAcronym() : "",
                    driver != null ? driver.teamName() : "",
                    totalPoints,
                    driverRaces.size(),
                    wins,
                    podiums,
                    dnfs,
                    bestFinish);
        }
    }
}
