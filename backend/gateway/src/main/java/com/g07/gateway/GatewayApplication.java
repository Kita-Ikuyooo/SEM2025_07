package com.g07.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

// 排除数据库自动配置
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// 精准扫描避开common里的数据库Handler
@ComponentScan(
    basePackages = {
        "com.g07.gateway",
        "com.g07.common.core",
        "com.g07.common.util",
        "com.g07.common.exception"
    }
)
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}