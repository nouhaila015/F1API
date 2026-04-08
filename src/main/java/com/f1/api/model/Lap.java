package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Lap(
        @JsonProperty("driver_number")
        int driverNumber,
        @JsonProperty("lap_duration")
        double lapDuration,
        @JsonProperty("session_key")
        int sessionKey
) {
}
