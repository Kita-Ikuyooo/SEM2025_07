package com.g07.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    // 生产环境建议从配置文件读取
    private static final String SECRET = "G07_Knowledge_Platform_Secret_Key_2025";
    private static final long EXPIRATION = 86400 * 1000; // 24小时

    /**
     * 生成令牌
     * @param userId 用户ID
     * @param tenantId 租户ID
     */
    public static String createToken(String userId, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /**
     * 解析令牌
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}