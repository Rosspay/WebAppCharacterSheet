package com.example.Back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
public class BackApplication {

    public static void main(String[] args) {
        ReactorDebugAgent.init();
        SpringApplication.run(BackApplication.class, args);
    }

}
