package com.f1.api.utils;

import com.f1.api.model.Lap;

import java.util.Comparator;
import java.util.List;

public class F1ScoringUtils {

    private static final int[] POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
    private static final int[] SPRINT_POINTS = {8, 7, 6, 5, 4, 3, 2, 1};
    private static final int FASTEST_LAP_MAX_POSITION = 10;

    private F1ScoringUtils() {}
    /**
     * Returns the points awarded for a Grand Prix finishing position.
     *
     * @param position     finishing position (1-based); 0 or negative = DNF/DNS/DSQ
     * @param fastestLap   whether the driver set the fastest lap
     */
    public static int calculatePoints(int position, boolean fastestLap) {
        if (position < 1 || position > POINTS.length) {
            return 0;
        }
        int points = POINTS[position - 1];
        if (fastestLap && position <= FASTEST_LAP_MAX_POSITION) {
            points += 1;
        }
        return points;
    }

    /**
     * Returns the points awarded for a Sprint race finishing position.
     * No fastest lap bonus is awarded in Sprint races.
     *
     * @param position finishing position (1-based); 0 or negative = DNF/DNS/DSQ
     */
    public static int calculateSprintPoints(int position) {
        if (position < 1 || position > SPRINT_POINTS.length) {
            return 0;
        }
        return SPRINT_POINTS[position - 1];
    }

    /**
     * Returns the driver number of the driver who set the fastest lap in the session.
     * Laps with a duration of 0 (pit laps, aborted laps) are ignored.
     *
     * @return the driver number, or -1 if no valid laps are found
     */
    public static int findFastestLapDriverNumber(List<Lap> laps) {
        return laps.stream()
                .filter(lap -> lap.lapDuration() != null && lap.lapDuration() > 0)
                .min(Comparator.comparingDouble(Lap::lapDuration))
                .map(Lap::driverNumber)
                .orElse(-1);
    }
}
