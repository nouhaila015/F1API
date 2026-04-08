package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionResult(
        boolean dnf,
        boolean dns,
        boolean dsq,
        @JsonProperty("driver_number")
        int driverNumber,
        double duration,
        @JsonProperty("gap_to_leader")
        String gapToLeader,
        @JsonProperty("number_of_laps")
        int numberOfLaps,
        int position,
        @JsonProperty("session_key")
        int sessionKey
) {
}
