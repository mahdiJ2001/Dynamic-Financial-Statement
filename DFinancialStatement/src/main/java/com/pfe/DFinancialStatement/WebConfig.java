package com.pfe.DFinancialStatement;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // Allow access to API paths
                .allowedOrigins("http://localhost:4200")  // Allow the Angular app running on this port
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Allow the methods you need
                .allowedHeaders("*");  // Allow all headers
    }
}
