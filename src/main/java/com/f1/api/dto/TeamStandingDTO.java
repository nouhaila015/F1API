package com.f1.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamStandingDTO {
    private int points;
    private String teamName;
    private String teamColour;
    private List<DriverSummaryDTO> drivers;
    private int position;
}
