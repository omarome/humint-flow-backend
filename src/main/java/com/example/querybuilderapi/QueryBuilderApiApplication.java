package com.example.querybuilderapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueryBuilderApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryBuilderApiApplication.class, args);
    }
}
