package com.f1.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverStandingDTO {
    private int position;
    private String firstName;
    private String lastName;
    private String nameAcronym;
    private String headshotsUrl;
    private int points;
}
