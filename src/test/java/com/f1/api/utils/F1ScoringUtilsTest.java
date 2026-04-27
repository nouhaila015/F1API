package com.f1.api.utils;

import com.f1.api.model.Lap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class F1ScoringUtilsTest {

    // --- calculatePoints ---

    @Test
    void calculatePoints_firstPlace_returns25() {
        assertThat(F1ScoringUtils.calculatePoints(1, false)).isEqualTo(25);
    }

    @Test
    void calculatePoints_tenthPlace_returns1() {
        assertThat(F1ScoringUtils.calculatePoints(10, false)).isEqualTo(1);
    }

    @Test
    void calculatePoints_eleventhPlace_returns0() {
        assertThat(F1ScoringUtils.calculatePoints(11, false)).isZero();
    }

    @Test
    void calculatePoints_zeroPosition_returns0() {
        assertThat(F1ScoringUtils.calculatePoints(0, false)).isZero();
    }

    @Test
    void calculatePoints_negativePosition_returns0() {
        assertThat(F1ScoringUtils.calculatePoints(-1, false)).isZero();
    }

    @Test
    void calculatePoints_firstPlaceWithFastestLap_returns26() {
        assertThat(F1ScoringUtils.calculatePoints(1, true)).isEqualTo(26);
    }

    @Test
    void calculatePoints_tenthPlaceWithFastestLap_returns2() {
        assertThat(F1ScoringUtils.calculatePoints(10, true)).isEqualTo(2);
    }

    @Test
    void calculatePoints_eleventhPlaceWithFastestLap_returns0() {
        // outside points, fastest lap bonus does not apply
        assertThat(F1ScoringUtils.calculatePoints(11, true)).isZero();
    }

    @Test
    void calculatePoints_allScoringPositions_matchExpectedPoints() {
        int[] expected = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        for (int i = 0; i < expected.length; i++) {
            assertThat(F1ScoringUtils.calculatePoints(i + 1, false))
                    .as("position %d", i + 1)
                    .isEqualTo(expected[i]);
        }
    }

    // --- calculateSprintPoints ---

    @Test
    void calculateSprintPoints_firstPlace_returns8() {
        assertThat(F1ScoringUtils.calculateSprintPoints(1)).isEqualTo(8);
    }

    @Test
    void calculateSprintPoints_eighthPlace_returns1() {
        assertThat(F1ScoringUtils.calculateSprintPoints(8)).isEqualTo(1);
    }

    @Test
    void calculateSprintPoints_ninthPlace_returns0() {
        assertThat(F1ScoringUtils.calculateSprintPoints(9)).isZero();
    }

    @Test
    void calculateSprintPoints_zeroPosition_returns0() {
        assertThat(F1ScoringUtils.calculateSprintPoints(0)).isZero();
    }

    @Test
    void calculateSprintPoints_negativePosition_returns0() {
        assertThat(F1ScoringUtils.calculateSprintPoints(-5)).isZero();
    }

    @Test
    void calculateSprintPoints_allScoringPositions_matchExpectedPoints() {
        int[] expected = {8, 7, 6, 5, 4, 3, 2, 1};
        for (int i = 0; i < expected.length; i++) {
            assertThat(F1ScoringUtils.calculateSprintPoints(i + 1))
                    .as("sprint position %d", i + 1)
                    .isEqualTo(expected[i]);
        }
    }

    // --- findFastestLapDriverNumber ---

    @Test
    void findFastestLapDriverNumber_singleLap_returnsThatDriver() {
        List<Lap> laps = List.of(new Lap(44, 85.123, 1));
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(laps)).isEqualTo(44);
    }

    @Test
    void findFastestLapDriverNumber_multipleLaps_returnsDriverWithShortestLap() {
        List<Lap> laps = List.of(
                new Lap(44, 85.5, 1),
                new Lap(16, 84.9, 1),
                new Lap(1,  86.0, 1)
        );
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(laps)).isEqualTo(16);
    }

    @Test
    void findFastestLapDriverNumber_emptyList_returnsMinusOne() {
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(List.of())).isEqualTo(-1);
    }

    @Test
    void findFastestLapDriverNumber_allZeroDurations_returnsMinusOne() {
        List<Lap> laps = List.of(
                new Lap(44, 0.0, 1),
                new Lap(16, 0.0, 1)
        );
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(laps)).isEqualTo(-1);
    }

    @Test
    void findFastestLapDriverNumber_nullDurationsIgnored_returnsValidDriver() {
        List<Lap> laps = List.of(
                new Lap(44, null, 1),
                new Lap(16, 84.9, 1)
        );
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(laps)).isEqualTo(16);
    }

    @Test
    void findFastestLapDriverNumber_mixedNullAndZeroDurations_returnsMinusOne() {
        List<Lap> laps = List.of(
                new Lap(44, null, 1),
                new Lap(16, 0.0,  1)
        );
        assertThat(F1ScoringUtils.findFastestLapDriverNumber(laps)).isEqualTo(-1);
    }
}
