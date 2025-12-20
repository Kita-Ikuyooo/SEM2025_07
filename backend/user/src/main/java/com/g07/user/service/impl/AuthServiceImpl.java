package com.g07.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.g07.common.core.R;
import com.g07.common.util.JwtUtil;
import com.g07.user.dto.LoginDto;
import com.g07.user.dto.RegisterDto;
import com.g07.user.entity.Tenant;
import com.g07.user.entity.User;
import com.g07.user.mapper.TenantMapper;
import com.g07.user.mapper.UserMapper;
import com.g07.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String login(LoginDto dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return JwtUtil.createToken(user.getUserId(), user.getTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterDto dto) {
        // 检查用户名是否已经存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) throw new RuntimeException("用户名已存在");

        // 创建租户
        Tenant tenant = new Tenant();
        tenant.setTenantName(dto.getTenantName());
        tenant.setStatus("active");
        tenant.setMaxStorage(10L * 1024 * 1024 * 1024);
        // tenant.setCreateTime(LocalDateTime.now());
        tenantMapper.insert(tenant);

        // 创建管理员
        User user = new User();
        user.setTenantId(tenant.getTenantId());
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole("admin");
        user.setIsActive(true);
        // user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        return user.getUserId();
    }
}