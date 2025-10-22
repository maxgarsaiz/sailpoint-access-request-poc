package com.sailpoint.accessrequest.infrastructure.config;

import com.sailpoint.accessrequest.infrastructure.client.SailpointFeignClient;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    @Value("${sailpoint.api.base-url}")
    private String sailpointBaseUrl;

    @Bean
    public SailpointFeignClient sailpointFeignClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(SailpointFeignClient.class))
                .logLevel(Logger.Level.FULL)
                .target(SailpointFeignClient.class, sailpointBaseUrl);
    }
}
