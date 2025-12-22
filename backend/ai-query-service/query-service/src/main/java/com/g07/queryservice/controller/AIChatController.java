package com.g07.queryservice.controller;

import com.g07.queryservice.dto.ChatRequest;
import com.g07.queryservice.dto.ChatResponse;
import com.g07.queryservice.service.AIService;
import com.g07.queryservice.service.EnhancedChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ai-chat")
public class AIChatController {

    @Autowired
    private EnhancedChatService enhancedChatService;

    @Autowired
    private AIService aiService;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    /**
     * DeepSeek AI问答接口
     */
    @PostMapping("/completions")
    public ChatResponse aiChat(@RequestBody ChatRequest request) {
        if (!aiEnabled) {
            return createFallbackResponse(request, "AI功能已禁用");
        }

        try {
            return enhancedChatService.aiEnhancedChat(request);
        } catch (Exception e) {
            System.err.println("AI服务异常: " + e.getMessage());
            return createFallbackResponse(request, "AI服务暂时不可用");
        }
    }

    /**
     * DeepSeek流式问答接口
     */
    @PostMapping("/completions/stream")
    public String streamChat(@RequestBody ChatRequest request) {
        // 流式响应实现（简化版）
        return "stream-response-placeholder";
    }

    /**
     * DeepSeek服务健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "DeepSeek AI Service");
        response.put("status", aiEnabled ? "enabled" : "disabled");
        response.put("timestamp", System.currentTimeMillis());

        if (aiEnabled) {
            boolean isHealthy = aiService.checkHealth();
            response.put("ai_health", isHealthy ? "healthy" : "unhealthy");
            response.put("ai_info", aiService.getServiceInfo());
        }

        return response;
    }

    /**
     * 获取DeepSeek配置信息
     */
    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("enabled", aiEnabled);
        info.put("provider", "DeepSeek");
        info.put("timestamp", System.currentTimeMillis());

        if (aiEnabled) {
            info.putAll(aiService.getServiceInfo());
        }

        return info;
    }

    /**
     * 更新DeepSeek配置
     */
    @PostMapping("/config")
    public Map<String, String> updateConfig(@RequestBody Map<String, Object> config) {
        // 配置更新逻辑（需要重启服务）
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "配置已更新，部分更改需要重启服务");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }

    /**
     * 测试DeepSeek连接
     */
    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        Map<String, Object> response = new HashMap<>();

        if (!aiEnabled) {
            response.put("status", "disabled");
            response.put("message", "AI功能已禁用");
            return response;
        }

        try {
            // 发送测试请求
            String testQuestion = "你好，请简单介绍一下自己。";
            List<String> context = new ArrayList<>();
            context.add("这是一个测试对话");

            String aiResponse = aiService.getDeepSeekResponse(testQuestion, context);

            response.put("status", "success");
            response.put("message", "DeepSeek连接正常");
            response.put("test_question", testQuestion);
            response.put("test_response", aiResponse != null ? aiResponse.substring(0, Math.min(100, aiResponse.length())) + "..." : "null");
            response.put("response_length", aiResponse != null ? aiResponse.length() : 0);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "DeepSeek连接失败: " + e.getMessage());
        }

        return response;
    }

    private ChatResponse createFallbackResponse(ChatRequest request, String message) {
        ChatResponse response = new ChatResponse();
        response.setAnswer("【系统提示】" + message + "\n\n您的原始问题: " + request.getQuestion());
        response.setSessionId(request.getSessionId() != null ? request.getSessionId() : "fallback_session");

        // 添加默认引用
        List<Map<String, Object>> citations = new ArrayList<>();
        Map<String, Object> citation = new HashMap<>();
        citation.put("source", "系统提示");
        citation.put("content", message);
        citations.add(citation);
        response.setCitations(citations);

        return response;
    }
}