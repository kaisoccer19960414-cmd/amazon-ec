package com.example.amazon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${dell.base-url}")
    private String dellBaseUrl;

    @Value("${smbc.base-url}")
    private String smbcBaseUrl;

    @Bean
    public WebClient dellWebClient() {
        return WebClient.builder()
                .baseUrl(dellBaseUrl)
                .build();
    }

    @Bean
    public WebClient smbcWebClient() {
        return WebClient.builder()
                .baseUrl(smbcBaseUrl)
                .build();
    }
}
