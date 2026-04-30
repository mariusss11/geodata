package com.geodata.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplateBean() {
        RestTemplate restTemplate = new RestTemplate();
        ClientHttpRequestInterceptor mdcInterceptor = (request, body, execution) -> {
            String requestId = MDC.get("requestId");
            if (requestId != null) {
                request.getHeaders().set("X-Request-ID", requestId);
            }
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(List.of(mdcInterceptor));
        return restTemplate;
    }
}
