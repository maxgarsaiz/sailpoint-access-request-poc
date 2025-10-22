package com.sailpoint.accessrequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class AccessRequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessRequestApplication.class, args);
    }
}
