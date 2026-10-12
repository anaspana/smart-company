package com.smartcompany.factory_simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FactorySimulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FactorySimulatorApplication.class, args);
    }
}