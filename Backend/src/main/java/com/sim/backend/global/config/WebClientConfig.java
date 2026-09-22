package com.sim.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public ExchangeStrategies webClientExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder(ExchangeStrategies strategies) {
        return WebClient.builder()
                .exchangeStrategies(strategies);
    }
}