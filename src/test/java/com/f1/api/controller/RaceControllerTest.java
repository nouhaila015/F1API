package com.f1.api.controller;

import com.f1.api.exception.OpenF1Exception;
import com.f1.api.model.Session;
import com.f1.api.model.dto.DriverStatsDTO;
import com.f1.api.model.dto.RaceResultDTO;
import com.f1.api.service.RaceService;
import com.f1.api.service.SessionService;
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

@WebMvcTest(RaceController.class)
class RaceControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RaceService raceService;
    @MockitoBean SessionService sessionService;

    @Test
    void getSeasonRaces_returnsJsonList() throws Exception {
        Session session = new Session("Monza", "IT", "Italy", null, null, 9001, "Race", "Race", 2025);
        when(sessionService.getSeasonRaces(2025)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/races/2025/season"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].session_key").value(9001))
                .andExpect(jsonPath("$[0].country_name").value("Italy"))
                .andExpect(jsonPath("$[0].session_type").value("Race"));
    }

    @Test
    void getSeasonRaces_emptyList_returns200WithEmptyArray() throws Exception {
        when(sessionService.getSeasonRaces(2025)).thenReturn(List.of());

        mockMvc.perform(get("/api/races/2025/season"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRaceResults_returnsJsonList() throws Exception {
        RaceResultDTO dto = new RaceResultDTO(1, 44, "Lewis Hamilton", "HAM",
                "Mercedes", "00D2BE", null, true, false, false, false);
        when(raceService.getRaceResults(9001)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].driverNumber").value(44))
                .andExpect(jsonPath("$[0].fullName").value("Lewis Hamilton"))
                .andExpect(jsonPath("$[0].fastestLap").value(true))
                .andExpect(jsonPath("$[0].dnf").value(false));
    }

    @Test
    void getRaceResults_emptySession_returns200WithEmptyArray() throws Exception {
        when(raceService.getRaceResults(9001)).thenReturn(List.of());

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getRaceResults_serviceThrowsOpenF1Exception_returns503() throws Exception {
        when(raceService.getRaceResults(9001)).thenThrow(new OpenF1Exception(429, "Rate limited"));

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Rate limited"));
    }

    @Test
    void getRaceResults_serviceThrows404OpenF1Exception_returns404() throws Exception {
        when(raceService.getRaceResults(9001)).thenThrow(new OpenF1Exception(404, "Not found"));

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRaceResults_serviceThrowsCompletionException_returns503() throws Exception {
        OpenF1Exception cause = new OpenF1Exception(429, "Rate limited");
        when(raceService.getRaceResults(9001)).thenThrow(new CompletionException(cause));

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getRaceResults_serviceThrowsCompletionExceptionWithUnknownCause_returns500() throws Exception {
        when(raceService.getRaceResults(9001))
                .thenThrow(new CompletionException(new RuntimeException("unexpected")));

        mockMvc.perform(get("/api/races/9001/results"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void getDriverStats_returnsDto() throws Exception {
        DriverStatsDTO dto = new DriverStatsDTO(44, "Lewis Hamilton", "HAM", "Mercedes",
                25, 1, 1, 1, 0, 1);
        when(raceService.getDriverStats(44, 2025)).thenReturn(dto);

        mockMvc.perform(get("/api/races/drivers/44/stats").param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverNumber").value(44))
                .andExpect(jsonPath("$.fullName").value("Lewis Hamilton"))
                .andExpect(jsonPath("$.totalPoints").value(25))
                .andExpect(jsonPath("$.wins").value(1))
                .andExpect(jsonPath("$.podiums").value(1))
                .andExpect(jsonPath("$.dnfs").value(0))
                .andExpect(jsonPath("$.bestFinish").value(1));
    }

    @Test
    void getDriverStats_missingYearParam_returns400() throws Exception {
        mockMvc.perform(get("/api/races/drivers/44/stats"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDriverStats_serviceThrowsOpenF1Exception_returns503() throws Exception {
        when(raceService.getDriverStats(44, 2025)).thenThrow(new OpenF1Exception(429, "Rate limited"));

        mockMvc.perform(get("/api/races/drivers/44/stats").param("year", "2025"))
                .andExpect(status().isServiceUnavailable());
    }
}
