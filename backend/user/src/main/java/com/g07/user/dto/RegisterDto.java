package com.g07.user.dto;
import lombok.Data;

@Data
public class RegisterDto {
    private String tenantName;
    private String username;
    private String password;
    private String email;
}