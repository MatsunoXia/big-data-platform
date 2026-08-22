package com.example.bigdata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.example.bigdata.mapper")
@EnableAsync
public class BigDataPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigDataPlatformApplication.class, args);
    }
}
