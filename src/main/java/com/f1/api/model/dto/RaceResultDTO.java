package com.f1.api.model.dto;

public record RaceResultDTO(
        int position,
        int driverNumber,
        String fullName,
        String nameAcronym,
        String teamName,
        String teamColour,
        Double gapToLeader,
        boolean fastestLap,
        boolean dnf,
        boolean dns,
        boolean dsq
) {
}
