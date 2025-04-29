package com.backend.crosswords;

import com.backend.crosswords.config.DigestGeneratorProperties;
import com.backend.crosswords.config.MailmanProperties;
import io.netty.channel.ChannelOption;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@EnableScheduling
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class CrosswordsApplication {
	private final MailmanProperties mailmanProperties;
	private final DigestGeneratorProperties digestGeneratorProperties;

	public CrosswordsApplication(MailmanProperties mailmanProperties, DigestGeneratorProperties digestGeneratorProperties) {
		this.mailmanProperties = mailmanProperties;
		this.digestGeneratorProperties = digestGeneratorProperties;
	}

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
	@Bean
	public WebClient mailmanWebClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder
				.baseUrl(mailmanProperties.getUrl())
				.clientConnector(new ReactorClientHttpConnector(
						HttpClient.create()
								.responseTimeout(Duration.ofMillis(mailmanProperties.getResponseTimeout()))
								.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, mailmanProperties.getConnectTimeout())
				))
				.build();
	}
	@Bean
	public WebClient digestGeneratorWebClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder
				.baseUrl(digestGeneratorProperties.getUrl())
				.clientConnector(new ReactorClientHttpConnector(
						HttpClient.create()
								.responseTimeout(Duration.ofMillis(digestGeneratorProperties.getResponseTimeout()))
								.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, digestGeneratorProperties.getConnectTimeout())
				))
				.build();
	}
}
