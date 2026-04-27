package com.f1.api.controller;

import com.f1.api.model.dto.DriverStandingDTO;
import com.f1.api.model.dto.TeamStandingDTO;
import com.f1.api.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@Tag(name = "Standings", description = "Driver and constructor championship standings")
public class StandingsController {

    private final ScoringService scoringService;

    public StandingsController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping("/{year}/drivers")
    @Operation(summary = "Get driver championship standings for a season")
    public List<DriverStandingDTO> getDriverStandings(
            @Parameter(description = "Championship year, e.g. 2024") @PathVariable int year) {
        return scoringService.getDriverStandings(year);
    }

    @GetMapping("/{year}/teams")
    @Operation(summary = "Get constructor championship standings for a season")
    public List<TeamStandingDTO> getTeamStandings(
            @Parameter(description = "Championship year, e.g. 2024") @PathVariable int year) {
        return scoringService.getTeamStandings(year);
    }
}
