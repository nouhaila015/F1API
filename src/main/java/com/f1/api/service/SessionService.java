package com.f1.api.service;

import com.f1.api.client.OpenF1Client;
import com.f1.api.model.Session;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class SessionService {

    private final OpenF1Client openF1Client;

    public SessionService(OpenF1Client openF1Client) {
        this.openF1Client = openF1Client;
    }

    @Cacheable("seasonRaces")
    public List<Session> getSeasonRaces(int year) {
        return openF1Client.getSessions(year).stream()
                .filter(s -> "Race".equalsIgnoreCase(s.sessionType()))
                .sorted(Comparator.comparing(Session::dateStart))
                .toList();
    }

    @Cacheable("pointsSessions")
    public List<Session> getPointsSessions(int year) {
        return openF1Client.getSessions(year).stream()
                .filter(s -> "Race".equalsIgnoreCase(s.sessionType()) || "Sprint".equalsIgnoreCase(s.sessionType()))
                .toList();
    }
}
