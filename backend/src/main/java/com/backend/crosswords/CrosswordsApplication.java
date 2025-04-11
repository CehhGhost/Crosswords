package com.backend.crosswords;

import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionLazyDelegator;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@EnableScheduling
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class CrosswordsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrosswordsApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.connectTimeout(Duration.ofSeconds(30))
				.readTimeout(Duration.ofSeconds(30))
				.build();
	}
}
