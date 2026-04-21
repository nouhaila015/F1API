package com.f1.api.controller;

import com.f1.api.model.dto.DriverStandingDTO;
import com.f1.api.model.dto.TeamStandingDTO;
import com.f1.api.service.ScoringService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
public class StandingsController {

    private final ScoringService scoringService;

    public StandingsController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping("/{year}/drivers")
    public List<DriverStandingDTO> getDriverStandings(@PathVariable int year) {
        return scoringService.getDriverStandings(year);
    }

    @GetMapping("/{year}/teams")
    public List<TeamStandingDTO> getTeamStandings(@PathVariable int year) {
        return scoringService.getTeamStandings(year);
    }
}
