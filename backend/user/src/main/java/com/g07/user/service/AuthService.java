package com.g07.user.service;
import com.g07.user.dto.LoginDto;
import com.g07.user.dto.RegisterDto;

public interface AuthService {
    String login(LoginDto dto);
    String register(RegisterDto dto);
}