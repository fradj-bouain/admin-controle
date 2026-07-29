package com.fluttiris.admincontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdminControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminControlApplication.class, args);
    }
}
