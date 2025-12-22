package com.g07.queryservice.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String sessionId;
    private String userId;
}