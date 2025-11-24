package com.fieldforcepro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@SpringBootApplication
@EnableSpringHttpSession
public class FieldForceProApplication {

    public static void main(String[] args) {
        SpringApplication.run(FieldForceProApplication.class, args);
    }
}
