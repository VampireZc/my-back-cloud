package com.zc.pay;

import client.DefaultFeignConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.zc.pay.mapper")
@SpringBootApplication
@EnableFeignClients(basePackages = "client", defaultConfiguration = DefaultFeignConfig.class)
public class payApplication {
    public static void main(String[] args) {
        SpringApplication.run(payApplication.class, args);
    }
}
