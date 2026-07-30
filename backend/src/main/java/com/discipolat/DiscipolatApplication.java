package com.discipolat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Modulith
@EnableScheduling
@EnableCaching
public class DiscipolatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscipolatApplication.class, args);
    }
}
