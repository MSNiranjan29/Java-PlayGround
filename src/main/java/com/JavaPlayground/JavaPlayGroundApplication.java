package com.JavaPlayground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.JavaPlayground.model")
@EnableJpaRepositories("com.JavaPlayground.repository")
public class JavaPlayGroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaPlayGroundApplication.class, args);
    }
}
