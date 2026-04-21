package com.f1.api.controller;

import com.f1.api.model.dto.DriverStatsDTO;
import com.f1.api.model.dto.RaceResultDTO;
import com.f1.api.model.Session;
import com.f1.api.service.RaceService;
import com.f1.api.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/races")
public class RaceController {

    private final RaceService raceService;
    private final SessionService sessionService;

    public RaceController(RaceService raceService, SessionService sessionService) {
        this.raceService = raceService;
        this.sessionService = sessionService;
    }

    @GetMapping("/{year}/season")
    public List<Session> getSeasonRaces(@PathVariable int year) {
        return sessionService.getSeasonRaces(year);
    }

    @GetMapping("/{sessionKey}/results")
    public List<RaceResultDTO> getRaceResults(@PathVariable int sessionKey) {
        return raceService.getRaceResults(sessionKey);
    }

    @GetMapping("/drivers/{driverNumber}/stats")
    public DriverStatsDTO getDriverStats(
            @PathVariable int driverNumber,
            @RequestParam int year) {
        return raceService.getDriverStats(driverNumber, year);
    }
}
