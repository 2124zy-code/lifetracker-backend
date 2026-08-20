package com.lifetracker;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lifetracker.modules.*.mapper")
@EnableScheduling
public class LifeTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeTrackerApplication.class, args);
        System.out.println("=================================================");
        System.out.println("🚀 LifeTracker Backend Service Started Successfully!");
        System.out.println("API Base URL: http://localhost:8080");
        System.out.println("=================================================");
    }
}
