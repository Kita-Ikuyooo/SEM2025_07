// src/main/java/com/g07/queryservice/dto/ChatResponse.java
package com.g07.queryservice.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

@Data
public class ChatResponse {
    private String answer;
    private String sessionId;
    private Long timestamp = System.currentTimeMillis();
    private List<Map<String, Object>> citations;
}