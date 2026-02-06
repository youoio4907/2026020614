// src/main/java/com/simvex/simvex_api/ai/OpenAIConfig.java
package com.simvex.simvex_api.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIConfig {

    @Bean
    public WebClient openAIWebClient(@Value("${openai.api-key:}") String apiKey) {
        WebClient.Builder b = WebClient.builder()
                .baseUrl("https://api.openai.com/v1");

        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        return b.build();
    }
}
