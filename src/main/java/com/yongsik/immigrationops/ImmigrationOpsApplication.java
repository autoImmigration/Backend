package com.yongsik.immigrationops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImmigrationOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImmigrationOpsApplication.class, args);
    }
}

