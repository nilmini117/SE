package com.driveflow.demo_driveflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DemoDriveflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoDriveflowApplication.class, args);
    }

}