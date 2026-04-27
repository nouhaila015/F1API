package com.f1.api.controller;

import com.f1.api.exception.OpenF1Exception;
import com.f1.api.model.dto.DriverStandingDTO;
import com.f1.api.model.dto.DriverSummaryDTO;
import com.f1.api.model.dto.TeamStandingDTO;
import com.f1.api.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CompletionException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StandingsController.class)
class StandingsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ScoringService scoringService;

    @Test
    void getDriverStandings_returnsJsonList() throws Exception {
        DriverStandingDTO dto = new DriverStandingDTO(1, "Max", "Verstappen", "VER", "http://img", 25);
        when(scoringService.getDriverStandings(2025)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Max"))
                .andExpect(jsonPath("$[0].lastName").value("Verstappen"))
                .andExpect(jsonPath("$[0].nameAcronym").value("VER"))
                .andExpect(jsonPath("$[0].points").value(25));
    }

    @Test
    void getDriverStandings_emptyList_returns200WithEmptyArray() throws Exception {
        when(scoringService.getDriverStandings(2025)).thenReturn(List.of());

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getDriverStandings_multipleDrivers_preservesOrder() throws Exception {
        DriverStandingDTO p1 = new DriverStandingDTO(1, "Max", "Verstappen", "VER", "", 25);
        DriverStandingDTO p2 = new DriverStandingDTO(2, "Lewis", "Hamilton", "HAM", "", 18);
        when(scoringService.getDriverStandings(2025)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].points").value(25))
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(jsonPath("$[1].points").value(18));
    }

    @Test
    void getDriverStandings_serviceThrowsOpenF1Exception_returns503() throws Exception {
        when(scoringService.getDriverStandings(2025)).thenThrow(new OpenF1Exception(429, "Rate limited"));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Rate limited"));
    }

    @Test
    void getDriverStandings_serviceThrowsNonRateLimitException_returnsUpstreamStatus() throws Exception {
        when(scoringService.getDriverStandings(2025)).thenThrow(new OpenF1Exception(500, "Upstream error"));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void getDriverStandings_serviceThrowsCompletionException_returns503() throws Exception {
        OpenF1Exception cause = new OpenF1Exception(429, "Rate limited");
        when(scoringService.getDriverStandings(2025)).thenThrow(new CompletionException(cause));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getDriverStandings_serviceThrowsCompletionExceptionWithUnknownCause_returns500() throws Exception {
        when(scoringService.getDriverStandings(2025))
                .thenThrow(new CompletionException(new RuntimeException("unexpected")));

        mockMvc.perform(get("/api/standings/2025/drivers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void getTeamStandings_returnsJsonList() throws Exception {
        List<DriverSummaryDTO> drivers = List.of(
                new DriverSummaryDTO("Max Verstappen", "VER", 1));
        TeamStandingDTO dto = new TeamStandingDTO(25, "Red Bull", "3671C6", drivers, 1);
        when(scoringService.getTeamStandings(2025)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/standings/2025/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].teamName").value("Red Bull"))
                .andExpect(jsonPath("$[0].teamColour").value("3671C6"))
                .andExpect(jsonPath("$[0].points").value(25))
                .andExpect(jsonPath("$[0].drivers.length()").value(1))
                .andExpect(jsonPath("$[0].drivers[0].acronym").value("VER"));
    }

    @Test
    void getTeamStandings_emptyList_returns200WithEmptyArray() throws Exception {
        when(scoringService.getTeamStandings(2025)).thenReturn(List.of());

        mockMvc.perform(get("/api/standings/2025/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTeamStandings_multipleTeams_preservesOrder() throws Exception {
        TeamStandingDTO t1 = new TeamStandingDTO(43, "Mercedes", "00D2BE", List.of(), 1);
        TeamStandingDTO t2 = new TeamStandingDTO(25, "Red Bull", "3671C6", List.of(), 2);
        when(scoringService.getTeamStandings(2025)).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/standings/2025/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamName").value("Mercedes"))
                .andExpect(jsonPath("$[0].points").value(43))
                .andExpect(jsonPath("$[1].teamName").value("Red Bull"))
                .andExpect(jsonPath("$[1].points").value(25));
    }

    @Test
    void getTeamStandings_serviceThrowsOpenF1Exception_returns503() throws Exception {
        when(scoringService.getTeamStandings(2025)).thenThrow(new OpenF1Exception(429, "Rate limited"));

        mockMvc.perform(get("/api/standings/2025/teams"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Rate limited"));
    }

    @Test
    void getTeamStandings_serviceThrowsCompletionException_returns503() throws Exception {
        OpenF1Exception cause = new OpenF1Exception(429, "Rate limited");
        when(scoringService.getTeamStandings(2025)).thenThrow(new CompletionException(cause));

        mockMvc.perform(get("/api/standings/2025/teams"))
                .andExpect(status().isServiceUnavailable());
    }
}
