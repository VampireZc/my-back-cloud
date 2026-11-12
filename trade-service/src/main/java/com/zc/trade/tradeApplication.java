package com.zc.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.zc.trade.mapper")
@SpringBootApplication
@EnableFeignClients(basePackages = "client")
public class tradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(tradeApplication.class, args);
    }
}