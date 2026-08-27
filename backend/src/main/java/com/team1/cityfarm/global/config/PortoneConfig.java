package com.team1.cityfarm.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "portone")
public class PortoneConfig {

    private String storeId;
    private String apiSecret;
    private String webhookSecret;
    private Api api = new Api();
    private ChannelKey channelKey = new ChannelKey();

    @Getter
    @Setter
    public static class Api {
        private String baseUrl = "https://api.portone.io";
    }

    @Getter
    @Setter
    public static class ChannelKey {
        private String normal;
        private String billing;
        private String identity;
    }

    @Bean
    public RestClient portoneRestClient() {
        return RestClient.builder()
                .baseUrl(api.getBaseUrl())
                .defaultHeader("Authorization", "Portone " + apiSecret)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}