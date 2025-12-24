package com.g07.controller;

import com.g07.common.R;
import com.g07.entity.User;
import com.g07.entity.Tenant;
import com.g07.mapper.UserMapper;
import com.g07.mapper.TenantMapper;
import com.g07.utils.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TenantMapper tenantMapper;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 登录接口：下发用户信息及Token
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        
        if (user != null && encoder.matches(password, user.getPasswordHash())) {
            String token = JwtUtils.createToken(user.getUserId(), user.getRole());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("role", user.getRole());
            data.put("username", user.getUsername());
            data.put("userId", user.getUserId());
            data.put("tenantId", user.getTenantId());
            data.put("email", user.getEmail());
            data.put("createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : "未知");
            
            return R.ok(data);
        }
        return R.error("用户名或密码错误");
    }

    /**
     * 注册接口：支持加入已有企业或创建新企业，且由用户自主选择角色
     */
    @PostMapping("/register")
    @Transactional
    public R<String> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String tenantName = body.get("tenantName");
        // 获取前端传来的角色，默认为 viewer
        String requestedRole = body.getOrDefault("role", "viewer");

        // 1. 检查用户名是否已占用
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (existingUser != null) {
            return R.error("该用户名已被注册");
        }

        // 2. 处理企业（租户）逻辑
        Tenant existingTenant = tenantMapper.selectOne(new QueryWrapper<Tenant>().eq("tenant_name", tenantName));
        String targetTenantId;

        if (existingTenant != null) {
            targetTenantId = existingTenant.getTenantId();
        } else {
            targetTenantId = UUID.randomUUID().toString().replace("-", "");
            Tenant newTenant = new Tenant();
            newTenant.setTenantId(targetTenantId);
            newTenant.setTenantName(tenantName);
            newTenant.setStatus("active");
            newTenant.setCreateTime(LocalDateTime.now());
            tenantMapper.insert(newTenant);
        }

        // 3. 创建新用户
        User user = new User();
        user.setUserId(UUID.randomUUID().toString().replace("-", ""));
        user.setTenantId(targetTenantId);
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(body.get("password")));
        user.setEmail(body.get("email"));

        // ================== 修改点：直接使用用户选择的角色 ==================
        // 简单校验一下，防止非法字符串
        if ("admin".equals(requestedRole) || "researcher".equals(requestedRole)) {
            user.setRole(requestedRole);
        } else {
            user.setRole("viewer"); // 兜底为普通用户
        }
        // ================================================================

        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        return R.ok(existingTenant != null ? "成功加入企业" : "注册成功，已创建新企业");
    }
}