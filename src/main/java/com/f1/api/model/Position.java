package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Position(
        @JsonProperty("driver_number")
        int driverNumber,
        int position,
        @JsonProperty("session_key")
        int sessionKey
) {
}
