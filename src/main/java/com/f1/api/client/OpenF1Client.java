package com.f1.api.client;

import com.f1.api.exception.OpenF1Exception;
import com.f1.api.model.Driver;
import com.f1.api.model.Lap;
import com.f1.api.model.Position;
import com.f1.api.model.Session;
import com.f1.api.model.SessionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.function.Function;

@Component
public class OpenF1Client {

    private static final Logger log = LoggerFactory.getLogger(OpenF1Client.class);
    private static final String SESSION_KEY = "session_key";

    private final WebClient webClient;

    public OpenF1Client(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Driver> getDrivers(int sessionKey) {
        return fetchList("/drivers", Driver.class, SESSION_KEY, sessionKey);
    }

    public List<Session> getSessions(int year) {
        return fetchList("/sessions", Session.class, "year", year);
    }

    public List<SessionResult> getSessionResults(int sessionKey) {
        return fetchList("/session_result", SessionResult.class, SESSION_KEY, sessionKey);
    }

    public List<Position> getPositions(int sessionKey) {
        return fetchList("/position", Position.class, SESSION_KEY ,sessionKey);
    }

    public List<Lap> getLaps(int sessionKey) {
        return fetchList("/laps", Lap.class, SESSION_KEY, sessionKey);
    }

    private <T> List<T> fetchList(String path, Class<T> type, String paramName, Object paramValue) {
        log.info("→ GET {}?{}={}", path, paramName, paramValue);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam(paramName, paramValue)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("<empty>")
                                .flatMap(body -> {
                                    log.info("429 from OpenF1 on {} — body: {}", path, body);
                                    return Mono.error(new OpenF1Exception(429, "Rate limited by OpenF1"));
                                }))
                .onStatus(status -> status.value() == 404,
                        response -> Mono.error(new OpenF1Exception(404, "Not found: " + path)))
                .onStatus(status -> status.is4xxClientError() && status.value() != 429 && status.value() != 404, toException(path))
                .onStatus(HttpStatusCode::is5xxServerError, toException(path))
                .bodyToFlux(type)
                .collectList()
                .onErrorReturn(e -> e instanceof OpenF1Exception ex && ex.getStatusCode() == 404, List.of())
                .block();
    }

    private Function<ClientResponse, Mono<? extends Throwable>> toException(String resource) {
        return response -> Mono.error(new OpenF1Exception(
                response.statusCode().value(),
                "OpenF1 error on " + resource + " — HTTP " + response.statusCode().value()));
    }
}
