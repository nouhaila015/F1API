package com.f1.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @Column(name = "session_key")
    private int sessionKey;

    @Column(name = "circuit_short_name")
    private String circuitShortName;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(name = "date_end")
    private OffsetDateTime dateEnd;

    @Column(name = "date_start")
    private OffsetDateTime dateStart;

    @Column(name = "session_name")
    private String sessionName;

    @Column(name = "session_type")
    private String sessionType;

    @Column(name = "year")
    private int year;

    @Column(name = "fastest_lap_driver_number")
    private Integer fastestLapDriverNumber;

    public SessionEntity() {}

    public SessionEntity(int sessionKey, String circuitShortName, String countryCode, String countryName,
                         OffsetDateTime dateEnd, OffsetDateTime dateStart, String sessionName,
                         String sessionType, int year, Integer fastestLapDriverNumber) {
        this.sessionKey = sessionKey;
        this.circuitShortName = circuitShortName;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.dateEnd = dateEnd;
        this.dateStart = dateStart;
        this.sessionName = sessionName;
        this.sessionType = sessionType;
        this.year = year;
        this.fastestLapDriverNumber = fastestLapDriverNumber;
    }

    public int getSessionKey() { return sessionKey; }
    public String getCircuitShortName() { return circuitShortName; }
    public String getCountryCode() { return countryCode; }
    public String getCountryName() { return countryName; }
    public OffsetDateTime getDateEnd() { return dateEnd; }
    public OffsetDateTime getDateStart() { return dateStart; }
    public String getSessionName() { return sessionName; }
    public String getSessionType() { return sessionType; }
    public int getYear() { return year; }
    public Integer getFastestLapDriverNumber() { return fastestLapDriverNumber; }
    public void setFastestLapDriverNumber(Integer fastestLapDriverNumber) {
        this.fastestLapDriverNumber = fastestLapDriverNumber;
    }
}
