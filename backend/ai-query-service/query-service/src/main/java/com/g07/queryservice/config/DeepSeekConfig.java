// 简单版 DeepSeekConfig.java
package com.g07.queryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class DeepSeekConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置超时（单位：毫秒）
        factory.setConnectTimeout(5000);     // 5秒连接超时
        factory.setReadTimeout(30000);       // 30秒读取超时

        RestTemplate restTemplate = new RestTemplate(factory);

        // 可选：添加日志拦截器
        restTemplate.getInterceptors().add((request, body, execution) -> {
            System.out.println("发送请求到: " + request.getURI());
            long startTime = System.currentTimeMillis();
            var response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("请求耗时: " + duration + "ms");
            return response;
        });

        return restTemplate;
    }
}