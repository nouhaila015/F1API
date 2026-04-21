package com.f1.api.model.dto;

public record DriverStatsDTO(
        int driverNumber,
        String fullName,
        String nameAcronym,
        String teamName,
        int totalPoints,
        int racesEntered,
        int wins,
        int podiums,
        int dnfs,
        Integer bestFinish
) {
}
