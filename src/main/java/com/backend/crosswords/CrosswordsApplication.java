package com.backend.crosswords;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class CrosswordsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrosswordsApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}
}
