package com.f1.api.controller;

import com.f1.api.model.dto.DriverStatsDTO;
import com.f1.api.model.dto.RaceResultDTO;
import com.f1.api.model.Session;
import com.f1.api.service.RaceService;
import com.f1.api.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/races")
@Tag(name = "Races", description = "Race sessions, results, and driver stats")
public class RaceController {

    private final RaceService raceService;
    private final SessionService sessionService;

    public RaceController(RaceService raceService, SessionService sessionService) {
        this.raceService = raceService;
        this.sessionService = sessionService;
    }

    @GetMapping("/{year}/season")
    @Operation(summary = "Get all race and sprint sessions for a season")
    public List<Session> getSeasonRaces(
            @Parameter(description = "Championship year, e.g. 2024") @PathVariable int year) {
        return sessionService.getSeasonRaces(year);
    }

    @GetMapping("/{sessionKey}/results")
    @Operation(summary = "Get results for a specific race or sprint session")
    public List<RaceResultDTO> getRaceResults(
            @Parameter(description = "OpenF1 session key") @PathVariable int sessionKey) {
        return raceService.getRaceResults(sessionKey);
    }

    @GetMapping("/drivers/{driverNumber}/stats")
    @Operation(summary = "Get aggregated stats for a driver across a season")
    public DriverStatsDTO getDriverStats(
            @Parameter(description = "Driver number, e.g. 1 for Verstappen") @PathVariable int driverNumber,
            @Parameter(description = "Championship year") @RequestParam int year) {
        return raceService.getDriverStats(driverNumber, year);
    }
}
