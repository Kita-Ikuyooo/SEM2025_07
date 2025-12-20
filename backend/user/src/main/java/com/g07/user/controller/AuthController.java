package com.g07.user.controller;

import com.g07.common.core.R;
import com.g07.user.dto.LoginDto;
import com.g07.user.dto.RegisterDto;
import com.g07.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/login")
    public R<String> login(@RequestBody LoginDto dto) {
        // try {
        //     return R.ok(authService.login(dto));
        // } catch (Exception e) {
        //     return R.fail(e.getMessage());
        // }
        // 直接返回，如果有异常会自动抛出给全局处理器
        return R.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public R<String> register(@RequestBody RegisterDto dto) {
        // try {
        //     return R.ok(authService.register(dto));
        // } catch (Exception e) {
        //     return R.fail(e.getMessage());
        // }
        return R.ok(authService.register(dto));
    }
}