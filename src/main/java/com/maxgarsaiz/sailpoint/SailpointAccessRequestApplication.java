package com.maxgarsaiz.sailpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SailpointAccessRequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SailpointAccessRequestApplication.class, args);
    }
}