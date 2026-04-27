package com.f1.api.service;

import com.f1.api.model.dto.DriverStandingDTO;
import com.f1.api.model.dto.TeamStandingDTO;
import com.f1.api.model.entity.DriverEntity;
import com.f1.api.model.entity.SessionEntity;
import com.f1.api.model.entity.SessionResultEntity;
import com.f1.api.repository.DriverRepository;
import com.f1.api.repository.SessionRepository;
import com.f1.api.repository.SessionResultRepository;
import com.f1.api.sync.DataSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock SessionRepository sessionRepository;
    @Mock DriverRepository driverRepository;
    @Mock SessionResultRepository sessionResultRepository;
    @Mock DataSyncService dataSyncService;

    ScoringService scoringService;

    private static final int YEAR = 2025;
    private static final int SK1 = 9001;
    private static final int SK2 = 9002;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService(sessionRepository, driverRepository,
                sessionResultRepository, dataSyncService);
    }

    // ────────────────────────────────────────────────
    // getDriverStandings
    // ────────────────────────────────────────────────

    @Test
    void getDriverStandings_noSessions_returnsEmpty() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of());

        assertThat(scoringService.getDriverStandings(YEAR)).isEmpty();
    }

    @Test
    void getDriverStandings_singleDriver_correctPoints() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity driver = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity result = result(SK1, 44, 1, false, false, false);

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(driver));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(result));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).getPoints()).isEqualTo(25);
        assertThat(standings.get(0).getPosition()).isEqualTo(1);
        assertThat(standings.get(0).getLastName()).isEqualTo("Hamilton");
    }

    @Test
    void getDriverStandings_sortedByPointsDescending() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        DriverEntity d1 = driver(SK1, 1, "Max", "Verstappen", "VER", "Red Bull");
        SessionResultEntity r44 = result(SK1, 44, 2, false, false, false); // 18 pts
        SessionResultEntity r1 = result(SK1, 1, 1, false, false, false);   // 25 pts

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44, d1));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44, r1));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings).hasSize(2);
        assertThat(standings.get(0).getPoints()).isEqualTo(25);
        assertThat(standings.get(0).getLastName()).isEqualTo("Verstappen");
        assertThat(standings.get(1).getPoints()).isEqualTo(18);
        assertThat(standings.get(1).getLastName()).isEqualTo("Hamilton");
    }

    @Test
    void getDriverStandings_positionsAssignedSequentially() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d1 = driver(SK1, 1, "Max", "Verstappen", "VER", "Red Bull");
        DriverEntity d16 = driver(SK1, 16, "Charles", "Leclerc", "LEC", "Ferrari");
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity r1 = result(SK1, 1, 1, false, false, false);    // 25
        SessionResultEntity r16 = result(SK1, 16, 2, false, false, false);  // 18
        SessionResultEntity r44 = result(SK1, 44, 3, false, false, false);  // 15

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d1, d16, d44));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r1, r16, r44));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings.get(0).getPosition()).isEqualTo(1);
        assertThat(standings.get(1).getPosition()).isEqualTo(2);
        assertThat(standings.get(2).getPosition()).isEqualTo(3);
    }

    @Test
    void getDriverStandings_fastestLapBonusApplied() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", 44); // driver 44 has fastest lap
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity r44 = result(SK1, 44, 1, false, false, false);

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings.get(0).getPoints()).isEqualTo(26); // 25 + 1 fastest lap
    }

    @Test
    void getDriverStandings_dnfDriver_getsZeroPoints() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity dnf = result(SK1, 44, 0, true, false, false);

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(dnf));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings.get(0).getPoints()).isZero();
    }

    @Test
    void getDriverStandings_sprintNoFastestLapBonus() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity sprint = raceSession(SK1, "Sprint", 44); // fastest lap stored but ignored for sprint
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity r44 = result(SK1, 44, 1, false, false, false);

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(sprint));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings.get(0).getPoints()).isEqualTo(8); // Sprint P1 = 8, no bonus
    }

    @Test
    void getDriverStandings_sprintAndRaceBothContribute() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        SessionEntity sprint = raceSession(SK2, "Sprint", null);
        DriverEntity dRace = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        DriverEntity dSprint = driver(SK2, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity raceResult = result(SK1, 44, 2, false, false, false);   // 18 pts
        SessionResultEntity sprintResult = result(SK2, 44, 1, false, false, false); // 8 pts

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race, sprint));
        when(driverRepository.findBySessionKeyIn(List.of(SK1, SK2)))
                .thenReturn(List.of(dRace, dSprint));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1, SK2)))
                .thenReturn(List.of(raceResult, sprintResult));

        List<DriverStandingDTO> standings = scoringService.getDriverStandings(YEAR);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).getPoints()).isEqualTo(26); // 18 + 8
    }

    // ────────────────────────────────────────────────
    // getTeamStandings
    // ────────────────────────────────────────────────

    @Test
    void getTeamStandings_noSessions_returnsEmpty() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of());

        assertThat(scoringService.getTeamStandings(YEAR)).isEmpty();
    }

    @Test
    void getTeamStandings_twoDriversSameTeam_pointsSummed() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        DriverEntity d63 = driver(SK1, 63, "George", "Russell", "RUS", "Mercedes");
        SessionResultEntity r44 = result(SK1, 44, 1, false, false, false); // 25 pts
        SessionResultEntity r63 = result(SK1, 63, 2, false, false, false); // 18 pts

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44, d63));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44, r63));

        List<TeamStandingDTO> standings = scoringService.getTeamStandings(YEAR);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).getTeamName()).isEqualTo("Mercedes");
        assertThat(standings.get(0).getPoints()).isEqualTo(43);
        assertThat(standings.get(0).getDrivers()).hasSize(2);
    }

    @Test
    void getTeamStandings_sortedByPointsDescending() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        DriverEntity d1 = driver(SK1, 1, "Max", "Verstappen", "VER", "Red Bull");
        SessionResultEntity r44 = result(SK1, 44, 2, false, false, false); // Mercedes: 18
        SessionResultEntity r1 = result(SK1, 1, 1, false, false, false);   // Red Bull: 25

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44, d1));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44, r1));

        List<TeamStandingDTO> standings = scoringService.getTeamStandings(YEAR);

        assertThat(standings).hasSize(2);
        assertThat(standings.get(0).getTeamName()).isEqualTo("Red Bull");
        assertThat(standings.get(0).getPoints()).isEqualTo(25);
        assertThat(standings.get(1).getTeamName()).isEqualTo("Mercedes");
        assertThat(standings.get(1).getPoints()).isEqualTo(18);
    }

    @Test
    void getTeamStandings_positionsAssignedSequentially() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d1 = driver(SK1, 1, "Max", "Verstappen", "VER", "Red Bull");
        DriverEntity d16 = driver(SK1, 16, "Charles", "Leclerc", "LEC", "Ferrari");
        SessionResultEntity r1 = result(SK1, 1, 1, false, false, false);   // 25
        SessionResultEntity r16 = result(SK1, 16, 2, false, false, false); // 18

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d1, d16));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r1, r16));

        List<TeamStandingDTO> standings = scoringService.getTeamStandings(YEAR);

        assertThat(standings.get(0).getPosition()).isEqualTo(1);
        assertThat(standings.get(1).getPosition()).isEqualTo(2);
    }

    @Test
    void getTeamStandings_teamColourPreserved() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(YEAR);
        SessionEntity race = raceSession(SK1, "Race", null);
        DriverEntity d44 = driver(SK1, 44, "Lewis", "Hamilton", "HAM", "Mercedes");
        SessionResultEntity r44 = result(SK1, 44, 3, false, false, false);

        when(sessionRepository.findByYearAndSessionTypeIn(YEAR, List.of("Race", "Sprint")))
                .thenReturn(List.of(race));
        when(driverRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(d44));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SK1))).thenReturn(List.of(r44));

        List<TeamStandingDTO> standings = scoringService.getTeamStandings(YEAR);

        assertThat(standings.get(0).getTeamColour()).isEqualTo("00D2BE");
    }

    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────

    private SessionEntity raceSession(int sessionKey, String sessionType, Integer fastestLapDriverNumber) {
        return new SessionEntity(sessionKey, "Monza", "IT", "Italy", null, null,
                sessionType, sessionType, YEAR, fastestLapDriverNumber);
    }

    private DriverEntity driver(int sessionKey, int driverNumber, String firstName, String lastName,
                                String acronym, String team) {
        return new DriverEntity(sessionKey, driverNumber, "GB", firstName,
                firstName + " " + lastName, lastName, "http://headshot", acronym, "00D2BE", team);
    }

    private SessionResultEntity result(int sessionKey, int driverNumber, int position,
                                      boolean dnf, boolean dns, boolean dsq) {
        return new SessionResultEntity(sessionKey, driverNumber, dnf, dns, dsq, null, null, 50, position);
    }
}
