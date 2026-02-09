package com.simvex.simvex_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // 모든 경로에 대해
                .allowedOriginPatterns("*")      // 모든 주소(ngrok, IP 등) 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 모든 방식 허용
                .allowedHeaders("*")             // 모든 헤더 허용
                .allowCredentials(true);         // 쿠키/인증정보 허용
    }
}