package com.f1.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class DriverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_key", nullable = false)
    private int sessionKey;

    @Column(name = "driver_number", nullable = false)
    private int driverNumber;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "headshot_url")
    private String headshotUrl;

    @Column(name = "name_acronym")
    private String nameAcronym;

    @Column(name = "team_colour")
    private String teamColour;

    @Column(name = "team_name")
    private String teamName;

    public DriverEntity() {}

    public DriverEntity(int sessionKey, int driverNumber, String countryCode, String firstName,
                        String fullName, String lastName, String headshotUrl, String nameAcronym,
                        String teamColour, String teamName) {
        this.sessionKey = sessionKey;
        this.driverNumber = driverNumber;
        this.countryCode = countryCode;
        this.firstName = firstName;
        this.fullName = fullName;
        this.lastName = lastName;
        this.headshotUrl = headshotUrl;
        this.nameAcronym = nameAcronym;
        this.teamColour = teamColour;
        this.teamName = teamName;
    }

    public Long getId() { return id; }
    public int getSessionKey() { return sessionKey; }
    public int getDriverNumber() { return driverNumber; }
    public String getCountryCode() { return countryCode; }
    public String getFirstName() { return firstName; }
    public String getFullName() { return fullName; }
    public String getLastName() { return lastName; }
    public String getHeadshotUrl() { return headshotUrl; }
    public String getNameAcronym() { return nameAcronym; }
    public String getTeamColour() { return teamColour; }
    public String getTeamName() { return teamName; }
}
