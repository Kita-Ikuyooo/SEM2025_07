package com.g07.gateway.filter;

import com.g07.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 白名单接口，无需登录即可访问
    private static final List<String> WHITELIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/chat/health",
            "/api/chat/test"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        for (String allowPath : WHITELIST) {
            if (pathMatcher.match(allowPath, path)) {
                return chain.filter(exchange);
            }
        }

        // 获取Token
        String token = request.getHeaders().getFirst("Authorization");

        // 校验Token存在性
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "未携带 Token 或格式错误");
        }

        //去掉"Bearer "
        token = token.substring(7);

        try {
            // 使用common模块工具解析Token
            Claims claims = JwtUtil.parseToken(token);
            
            String userId = claims.getSubject();
            String tenantId = (String) claims.get("tenant_id");

            if (userId == null || tenantId == null) {
                return unauthorized(exchange, "Token 无效：缺失关键身份信息");
            }

            // 透传Header给下游服务
            // 下游服务可以通过request.getHeader("X-Tenant-Id")获取租户ID
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Tenant-Id", tenantId)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return unauthorized(exchange, "Token 已过期或无效");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String json = String.format("{\"code\": 401, \"msg\": \"%s\", \"data\": null}", msg);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 优先级
        return -100;
    }
}