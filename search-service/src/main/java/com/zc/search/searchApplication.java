package com.zc.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 复用 item-service 模块的 IItemService：需要扫描其 service 包，并注册它的 Mapper
@MapperScan("com.zc.item.mapper")
@SpringBootApplication(scanBasePackages = {"com.zc.search", "com.zc.item.service"})
public class searchApplication {
    public static void main(String[] args) {
        SpringApplication.run(searchApplication.class, args);
    }
}
