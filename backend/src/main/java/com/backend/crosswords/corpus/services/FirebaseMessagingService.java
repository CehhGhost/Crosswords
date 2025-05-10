package com.backend.crosswords.corpus.services;

import org.apache.http.ConnectionClosedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class FirebaseMessagingService {
    private final WebClient firebaseWebClient;
    @Value("${crosswords-firebase-projectid}")
    private String projectId;

    public FirebaseMessagingService(@Qualifier("firebaseWebClient") WebClient firebaseWebClient) {
        this.firebaseWebClient = firebaseWebClient;
    }

    public Mono<Void> sendNotification(String token, String subscriptionTitle, Map<String, String> data) throws ConnectionClosedException {
        String url = String.format("/projects/%s/messages:send", projectId);

        Map<String, Object> message = Map.of(
                "message", Map.of(
                        "token", token,
                        "notification", Map.of(
                                "title", "Новый дайджест!",
                                "body", "Новый дайджест по теме: " + subscriptionTitle
                        ),
                        "data", data
                )
        );

        try {
            return firebaseWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(response -> System.out.println("FCM response: " + response))
                    .then();
        } catch (Exception e) {
            throw new ConnectionClosedException(e.getMessage());
        }
    }
}
