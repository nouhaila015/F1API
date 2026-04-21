package com.f1.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_results")
public class SessionResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_key", nullable = false)
    private int sessionKey;

    @Column(name = "driver_number", nullable = false)
    private int driverNumber;

    @Column(name = "dnf", nullable = false)
    private boolean dnf;

    @Column(name = "dns", nullable = false)
    private boolean dns;

    @Column(name = "dsq", nullable = false)
    private boolean dsq;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "gap_to_leader")
    private Double gapToLeader;

    @Column(name = "number_of_laps", nullable = false)
    private int numberOfLaps;

    @Column(name = "position")
    private Integer position;

    public SessionResultEntity() {}

    public SessionResultEntity(int sessionKey, int driverNumber, boolean dnf, boolean dns, boolean dsq,
                               Double duration, Double gapToLeader, int numberOfLaps, Integer position) {
        this.sessionKey = sessionKey;
        this.driverNumber = driverNumber;
        this.dnf = dnf;
        this.dns = dns;
        this.dsq = dsq;
        this.duration = duration;
        this.gapToLeader = gapToLeader;
        this.numberOfLaps = numberOfLaps;
        this.position = position;
    }

    public Long getId() { return id; }
    public int getSessionKey() { return sessionKey; }
    public int getDriverNumber() { return driverNumber; }
    public boolean isDnf() { return dnf; }
    public boolean isDns() { return dns; }
    public boolean isDsq() { return dsq; }
    public Double getDuration() { return duration; }
    public Double getGapToLeader() { return gapToLeader; }
    public int getNumberOfLaps() { return numberOfLaps; }
    public Integer getPosition() { return position; }
}
