package com.ideafly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 拦截所有 /api/** 路径的请求
                .allowedOrigins("http://localhost:6365") // 允许来自 http://localhost:6365 的跨域请求 (明确指定)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 允许携带 Cookie (现在 origins 是明确指定的，所以可以设置为 true)
                .maxAge(3600);
    }
}