package org.example.profiler.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ProfilerServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProfilerServerApplication.class, args);
    }
}