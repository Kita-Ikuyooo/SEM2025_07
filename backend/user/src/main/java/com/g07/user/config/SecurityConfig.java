package com.g07.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置拦截规则
            .authorizeHttpRequests(auth -> auth
                // 放行登录和注册接口
                .requestMatchers("/api/auth/**").permitAll()
                // 其他接口需要认证
                .anyRequest().authenticated()
            )
            
            // 异常处理，未登录或权限不足时返回JSON
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    writeJson(response, 401, "未授权，请先登录");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    writeJson(response, 403, "权限不足，禁止访问");
                })
            );

        return http.build();
    }

    /**
     * 用于向客户端响应 JSON 数据
     */
    private void writeJson(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        
        // 构建简单的JSON字符串: {"code": status, "msg": "xxx", "data": null}
        Map<String, Object> map = new HashMap<>();
        map.put("code", status);
        map.put("msg", msg);
        map.put("data", null);

        // 使用Jackson将Map转为JSON字符串
        String json = new ObjectMapper().writeValueAsString(map);
        
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
        writer.close();
    }
}