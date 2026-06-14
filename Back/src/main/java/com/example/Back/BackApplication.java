package com.example.Back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;
/**
 * Spring Boot entry point for the server application.
 */

@SpringBootApplication
public class BackApplication {

    public static void main(String[] args) {
        ReactorDebugAgent.init();
        SpringApplication.run(BackApplication.class, args);
    }

}
