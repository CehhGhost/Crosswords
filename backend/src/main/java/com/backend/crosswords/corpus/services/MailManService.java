package com.backend.crosswords.corpus.services;

import com.backend.crosswords.config.MailmanProperties;
import com.backend.crosswords.corpus.dto.SendDigestByEmailsDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MailManService {
    private final WebClient webClient;
    private final MailmanProperties properties;

    public MailManService(@Qualifier("mailmanWebClient")WebClient webClient, MailmanProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    public Mono<Boolean> checkServiceAvailability() {
        return webClient.get()
                .uri(properties.getCheckHealthPath())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(true);
                    } else {
                        return Mono.just(false);
                    }
                })
                .onErrorReturn(false);
    }
    public Mono<String> sendEmail(SendDigestByEmailsDTO request) {
        return webClient.post()
                .uri(properties.getSendEmailPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, error -> Mono.error(new RuntimeException("Connection error")))
                .bodyToMono(String.class);
    }
}
