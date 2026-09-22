package com.sim.backend.domain.nft.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PinataConfig {

  @Bean
  public WebClient pinataApiClient(WebClient.Builder builder,
                                   @Value("${pinata.api.base}") String apiBase,
                                   @Value("${pinata.jwt}") String jwt) {
    return builder
            .baseUrl(apiBase)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .build();
  }
}