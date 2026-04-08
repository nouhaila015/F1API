package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record Session(
        @JsonProperty("circuit_short_name")
        String circuitShortName,
        @JsonProperty("country_code")
        String countryCode,
        @JsonProperty("country_name")
        String countryName,
        @JsonProperty("date_end")
        OffsetDateTime dateEnd,
        @JsonProperty("date_start")
        OffsetDateTime dateStart,
        @JsonProperty("session_key")
        int sessionKey,
        @JsonProperty("session_name")
        String sessionName,
        @JsonProperty("session_type")
        String sessionType,
        int year
) {
}
