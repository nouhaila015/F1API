package com.f1.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Driver(
        @JsonProperty("country_code")
        String countryCode,
        @JsonProperty("driver_number")
        int driverNumber,
        @JsonProperty("first_name")
        String firstName,
        @JsonProperty("full_name")
        String fullName,
        @JsonProperty("last_name")
        String lastName,
        @JsonProperty("headshot_url")
        String headshotURL,
        @JsonProperty("name_acronym")
        String nameAcronym,
        @JsonProperty("team_colour")
        String teamColour,
        @JsonProperty("team_name")
        String teamName
) {
}
