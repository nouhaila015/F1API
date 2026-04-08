package com.f1.api.dto;

public record RaceResultDTO(
        int position,
        int driverNumber,
        String fullName,
        String nameAcronym,
        String teamName,
        String teamColour,
        String gapToLeader,
        boolean fastestLap,
        boolean dnf,
        boolean dns,
        boolean dsq
) {
}
