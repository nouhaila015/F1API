package com.f1.api.client;

import com.f1.api.exception.OpenF1Exception;
import com.f1.api.model.Driver;
import com.f1.api.model.Position;
import com.f1.api.model.Session;
import com.f1.api.model.SessionResult;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class OpenF1Client {

    private final WebClient webClient;

    public OpenF1Client(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Driver> getDrivers(int sessionKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/drivers")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, toException("drivers"))
                .onStatus(HttpStatusCode::is5xxServerError, toException("drivers"))
                .bodyToFlux(Driver.class)
                .collectList()
                .block();
    }

    public List<Session> getSessions(int year) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sessions")
                        .queryParam("year", year)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, toException("sessions"))
                .onStatus(HttpStatusCode::is5xxServerError, toException("sessions"))
                .bodyToFlux(Session.class)
                .collectList()
                .block();
    }

    public List<SessionResult> getSessionResults(int sessionKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/session_results")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, toException("session_results"))
                .onStatus(HttpStatusCode::is5xxServerError, toException("session_results"))
                .bodyToFlux(SessionResult.class)
                .collectList()
                .block();
    }

    public List<Position> getPositions(int sessionKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/position")
                        .queryParam("session_key", sessionKey)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, toException("position"))
                .onStatus(HttpStatusCode::is5xxServerError, toException("position"))
                .bodyToFlux(Position.class)
                .collectList()
                .block();
    }

    private java.util.function.Function<ClientResponse, Mono<? extends Throwable>> toException(String resource) {
        return response -> Mono.error(new OpenF1Exception(
                response.statusCode().value(),
                "OpenF1 error on /" + resource + " — HTTP " + response.statusCode().value()));
    }
}
