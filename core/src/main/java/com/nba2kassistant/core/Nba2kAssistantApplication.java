package com.nba2kassistant.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Nba2kAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(Nba2kAssistantApplication.class, args);
    }
}
