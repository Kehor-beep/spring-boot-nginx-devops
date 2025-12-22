package com.example.springwebapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringWebAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebAppApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Hello from Spring Boot behind Nginx 🚀";
    }
}
