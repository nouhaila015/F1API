package com.f1.api.service;

import com.f1.api.model.dto.DriverStatsDTO;
import com.f1.api.model.dto.RaceResultDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceServiceTest {

    @Mock SessionRepository sessionRepository;
    @Mock DriverRepository driverRepository;
    @Mock SessionResultRepository sessionResultRepository;
    @Mock DataSyncService dataSyncService;

    RaceService raceService;

    // Shared session key
    private static final int SESSION_KEY = 9001;

    @BeforeEach
    void setUp() {
        raceService = new RaceService(sessionRepository, driverRepository, sessionResultRepository, dataSyncService);
    }

    // ────────────────────────────────────────────────
    // getRaceResults
    // ────────────────────────────────────────────────

    @Test
    void getRaceResults_withResults_returnsMappedDTOs() {
        DriverEntity driver = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");
        SessionResultEntity result = result(SESSION_KEY, 44, 1, false, false, false, 1.5);
        SessionEntity session = session(SESSION_KEY, 44);

        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(driver));
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.of(session));
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(result));

        List<RaceResultDTO> dtos = raceService.getRaceResults(SESSION_KEY);

        assertThat(dtos).hasSize(1);
        RaceResultDTO dto = dtos.get(0);
        assertThat(dto.position()).isEqualTo(1);
        assertThat(dto.driverNumber()).isEqualTo(44);
        assertThat(dto.fullName()).isEqualTo("Lewis Hamilton");
        assertThat(dto.nameAcronym()).isEqualTo("HAM");
        assertThat(dto.teamName()).isEqualTo("Mercedes");
        assertThat(dto.teamColour()).isEqualTo("00D2BE");
        assertThat(dto.gapToLeader()).isEqualTo(1.5);
        assertThat(dto.fastestLap()).isTrue();
        assertThat(dto.dnf()).isFalse();
    }

    @Test
    void getRaceResults_unknownDriver_usesDefaultValues() {
        // No driver in driverRepository for this driver number
        SessionResultEntity result = result(SESSION_KEY, 99, 3, false, false, false, null);
        SessionEntity session = session(SESSION_KEY, -1);

        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of());
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.of(session));
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(result));

        List<RaceResultDTO> dtos = raceService.getRaceResults(SESSION_KEY);

        assertThat(dtos).hasSize(1);
        RaceResultDTO dto = dtos.get(0);
        assertThat(dto.fullName()).isEqualTo("Unknown");
        assertThat(dto.nameAcronym()).isEmpty();
        assertThat(dto.teamName()).isEmpty();
        assertThat(dto.teamColour()).isEmpty();
    }

    @Test
    void getRaceResults_noFastestLapInSession_fastestLapFalseForAll() {
        DriverEntity driver = driver(SESSION_KEY, 16, "Charles Leclerc", "LEC", "Ferrari", "DC0000");
        SessionResultEntity result = result(SESSION_KEY, 16, 1, false, false, false, null);
        // Session with no fastest lap driver (null → resolved to -1)
        SessionEntity session = session(SESSION_KEY, null);

        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(driver));
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.of(session));
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(result));

        List<RaceResultDTO> dtos = raceService.getRaceResults(SESSION_KEY);

        assertThat(dtos.get(0).fastestLap()).isFalse();
    }

    @Test
    void getRaceResults_sessionNotFound_fastestLapFalse() {
        DriverEntity driver = driver(SESSION_KEY, 1, "Max Verstappen", "VER", "Red Bull", "3671C6");
        SessionResultEntity result = result(SESSION_KEY, 1, 1, false, false, false, null);

        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(driver));
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.empty());
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(result));

        List<RaceResultDTO> dtos = raceService.getRaceResults(SESSION_KEY);

        assertThat(dtos.get(0).fastestLap()).isFalse();
    }

    @Test
    void getRaceResults_dnfDrivers_sortedAfterFinishers() {
        DriverEntity d1 = driver(SESSION_KEY, 1, "Max Verstappen", "VER", "Red Bull", "3671C6");
        DriverEntity d16 = driver(SESSION_KEY, 16, "Charles Leclerc", "LEC", "Ferrari", "DC0000");
        SessionResultEntity r1 = result(SESSION_KEY, 1, 1, false, false, false, null);
        SessionResultEntity rDnf = result(SESSION_KEY, 16, 0, true, false, false, null);
        SessionEntity session = session(SESSION_KEY, -1);

        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(d1, d16));
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.of(session));
        // Return DNF first to verify sorting
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of(rDnf, r1));

        List<RaceResultDTO> dtos = raceService.getRaceResults(SESSION_KEY);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).driverNumber()).isEqualTo(1);   // finisher first
        assertThat(dtos.get(1).driverNumber()).isEqualTo(16);  // DNF last
        assertThat(dtos.get(1).dnf()).isTrue();
    }

    @Test
    void getRaceResults_emptyResults_returnsEmptyList() {
        when(driverRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of());
        when(sessionRepository.findById(SESSION_KEY)).thenReturn(Optional.empty());
        when(sessionResultRepository.findBySessionKey(SESSION_KEY)).thenReturn(List.of());

        assertThat(raceService.getRaceResults(SESSION_KEY)).isEmpty();
    }

    // ────────────────────────────────────────────────
    // getDriverStats
    // ────────────────────────────────────────────────

    @Test
    void getDriverStats_noSessions_returnsEmptyDTO() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race")).thenReturn(List.of());

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.totalPoints()).isZero();
        assertThat(dto.racesEntered()).isZero();
        assertThat(dto.bestFinish()).isNull();
    }

    @Test
    void getDriverStats_driverNotInAnyResult_returnsEmptyDTO() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        SessionEntity raceSession = raceSession(SESSION_KEY, "Race", 2025, 44);
        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(raceSession));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY))).thenReturn(List.of());

        DriverStatsDTO dto = raceService.getDriverStats(16, 2025);

        assertThat(dto.totalPoints()).isZero();
        assertThat(dto.racesEntered()).isZero();
    }

    @Test
    void getDriverStats_singleWin_countsCorrectly() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        SessionEntity raceSession = raceSession(SESSION_KEY, "Race", 2025, 44);
        SessionResultEntity winResult = result(SESSION_KEY, 44, 1, false, false, false, null);
        DriverEntity driverEntity = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");

        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(raceSession));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY)))
                .thenReturn(List.of(winResult));
        when(driverRepository.findBySessionKeyAndDriverNumber(SESSION_KEY, 44))
                .thenReturn(Optional.of(driverEntity));

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.totalPoints()).isEqualTo(26); // 25 + 1 fastest lap bonus (driver 44 == fastestLapDriver 44)
        assertThat(dto.wins()).isEqualTo(1);
        assertThat(dto.podiums()).isEqualTo(1);
        assertThat(dto.dnfs()).isZero();
        assertThat(dto.bestFinish()).isEqualTo(1);
        assertThat(dto.racesEntered()).isEqualTo(1);
        assertThat(dto.fullName()).isEqualTo("Lewis Hamilton");
    }

    @Test
    void getDriverStats_podiumWithoutWin_noWinCounted() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        SessionEntity raceSession = raceSession(SESSION_KEY, "Race", 2025, -1);
        SessionResultEntity p3 = result(SESSION_KEY, 44, 3, false, false, false, null);
        DriverEntity driverEntity = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");

        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(raceSession));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY)))
                .thenReturn(List.of(p3));
        when(driverRepository.findBySessionKeyAndDriverNumber(SESSION_KEY, 44))
                .thenReturn(Optional.of(driverEntity));

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.wins()).isZero();
        assertThat(dto.podiums()).isEqualTo(1);
        assertThat(dto.totalPoints()).isEqualTo(15);
        assertThat(dto.bestFinish()).isEqualTo(3);
    }

    @Test
    void getDriverStats_dnf_countedInDnfsNotPoints() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        SessionEntity raceSession = raceSession(SESSION_KEY, "Race", 2025, -1);
        SessionResultEntity dnfResult = result(SESSION_KEY, 44, 0, true, false, false, null);
        DriverEntity driverEntity = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");

        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(raceSession));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY)))
                .thenReturn(List.of(dnfResult));
        when(driverRepository.findBySessionKeyAndDriverNumber(SESSION_KEY, 44))
                .thenReturn(Optional.of(driverEntity));

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.dnfs()).isEqualTo(1);
        assertThat(dto.totalPoints()).isZero();
        assertThat(dto.wins()).isZero();
        assertThat(dto.bestFinish()).isNull();
    }

    @Test
    void getDriverStats_sprintSession_pointsCountedNoWinOrPodium() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        SessionEntity sprintSession = raceSession(SESSION_KEY, "Sprint", 2025, -1);
        SessionResultEntity p1Sprint = result(SESSION_KEY, 44, 1, false, false, false, null);
        DriverEntity driverEntity = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");

        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(sprintSession));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY)))
                .thenReturn(List.of(p1Sprint));
        when(driverRepository.findBySessionKeyAndDriverNumber(SESSION_KEY, 44))
                .thenReturn(Optional.of(driverEntity));

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.totalPoints()).isEqualTo(8); // Sprint P1 = 8 pts
        assertThat(dto.wins()).isZero();            // Sprint wins don't count
        assertThat(dto.podiums()).isZero();         // Sprint podiums don't count
        assertThat(dto.bestFinish()).isNull();      // Sprint bestFinish not tracked
    }

    @Test
    void getDriverStats_multipleRaces_accumulatesAll() {
        doNothing().when(dataSyncService).triggerSyncIfNeeded(2025);
        int sk2 = 9002;
        SessionEntity race1 = raceSession(SESSION_KEY, "Race", 2025, -1);
        SessionEntity race2 = raceSession(sk2, "Race", 2025, -1);
        SessionResultEntity r1 = result(SESSION_KEY, 44, 2, false, false, false, null);
        SessionResultEntity r2 = result(sk2, 44, 5, false, false, false, null);
        DriverEntity driverEntity = driver(SESSION_KEY, 44, "Lewis Hamilton", "HAM", "Mercedes", "00D2BE");

        when(sessionRepository.findByYearAndSessionTypeOrderByDateStartAsc(2025, "Race"))
                .thenReturn(List.of(race1, race2));
        when(sessionResultRepository.findBySessionKeyIn(List.of(SESSION_KEY, sk2)))
                .thenReturn(List.of(r1, r2));
        when(driverRepository.findBySessionKeyAndDriverNumber(SESSION_KEY, 44))
                .thenReturn(Optional.of(driverEntity));

        DriverStatsDTO dto = raceService.getDriverStats(44, 2025);

        assertThat(dto.totalPoints()).isEqualTo(18 + 10); // P2 + P5
        assertThat(dto.racesEntered()).isEqualTo(2);
        assertThat(dto.wins()).isZero();
        assertThat(dto.podiums()).isEqualTo(1); // only P2 counts as podium
        assertThat(dto.bestFinish()).isEqualTo(2);
    }

    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────

    private DriverEntity driver(int sessionKey, int driverNumber, String fullName, String acronym,
                                String team, String colour) {
        return new DriverEntity(sessionKey, driverNumber, "GB", "First", fullName, "Last",
                "http://headshot", acronym, colour, team);
    }

    private SessionResultEntity result(int sessionKey, int driverNumber, int position,
                                       boolean dnf, boolean dns, boolean dsq, Double gap) {
        return new SessionResultEntity(sessionKey, driverNumber, dnf, dns, dsq, null, gap, 50, position);
    }

    /** Session where fastestLapDriverNumber is an int (convenience) */
    private SessionEntity session(int sessionKey, Integer fastestLapDriverNumber) {
        return new SessionEntity(sessionKey, "Monza", "IT", "Italy", null, null,
                "Race", "Race", 2025, fastestLapDriverNumber);
    }

    /** Session for getDriverStats (sessionName drives sprint detection) */
    private SessionEntity raceSession(int sessionKey, String sessionName, int year, Integer fastestLapDriver) {
        return new SessionEntity(sessionKey, "Monza", "IT", "Italy", null, null,
                sessionName, sessionName, year, fastestLapDriver);
    }
}
