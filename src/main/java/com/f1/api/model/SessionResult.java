package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.f1.api.utils.GapDeserializer;

public record SessionResult(
        boolean dnf,
        boolean dns,
        boolean dsq,
        @JsonProperty("driver_number")
        int driverNumber,
        Double duration,
        @JsonProperty("gap_to_leader")
        @JsonDeserialize(using = GapDeserializer.class)
        Double gapToLeader,
        @JsonProperty("number_of_laps")
        int numberOfLaps,
        Integer position,
        @JsonProperty("session_key")
        int sessionKey
) {
}
