package com.team1.cityfarm;

import com.team1.cityfarm.global.exception.CustomError;
import com.team1.cityfarm.global.exception.CustomException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CityfarmApplication {

    public static void main(String[] args) {

        SpringApplication.run(CityfarmApplication.class, args);
    }

}
