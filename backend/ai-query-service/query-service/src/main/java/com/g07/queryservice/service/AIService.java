package com.g07.queryservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class AIService {

    @Value("${ai.api.url:https://api.deepseek.com}")
    private String aiApiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @Value("${ai.max_tokens:2048}")
    private int maxTokens;

    @Value("${ai.temperature:0.7}")
    private double temperature;

    private final RestTemplate restTemplate;

    public AIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用DeepSeek API获取回答
     */
    public String getDeepSeekResponse(String question, List<String> context) {
        // 构建DeepSeek格式的请求
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示词
        String systemPrompt = buildSystemPrompt(context);
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // 用户问题
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", question);
        messages.add(userMessage);

        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", messages);
        request.put("temperature", temperature);
        request.put("max_tokens", maxTokens);
        request.put("stream", false); // 非流式响应

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            // 调用DeepSeek API
            String apiEndpoint = aiApiUrl + "/chat/completions";

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    apiEndpoint,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> response = responseEntity.getBody();

                // 解析DeepSeek响应格式
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("DeepSeek API调用失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(List<String> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的产业知识问答助手。请基于以下上下文信息回答用户的问题。\n");
        prompt.append("请确保回答专业、准确、简洁。\n");
        prompt.append("如果问题不在上下文中，请基于你的知识回答，并说明这是通用知识。\n\n");

        if (context != null && !context.isEmpty()) {
            prompt.append("【上下文信息】\n");
            for (int i = 0; i < context.size(); i++) {
                prompt.append(i + 1).append(". ").append(context.get(i)).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("回答要求：\n");
        prompt.append("1. 直接回答问题，不要重复问题\n");
        prompt.append("2. 如果适用，可以分点说明\n");
        prompt.append("3. 保持专业但易于理解\n");

        return prompt.toString();
    }

    /**
     * 获取向量化的上下文（占位实现）
     */
    public List<String> retrieveRelevantContext(String question) {
        // TODO: 实现向量检索
        // 目前返回模拟数据
        List<String> context = new ArrayList<>();
        context.add("人工智能是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。");
        context.add("机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习和改进，而无需明确编程。");
        context.add("深度学习是机器学习的一个子集，它使用多层神经网络来模拟人脑的复杂模式识别能力。");
        return context;
    }

    /**
     * 健康检查 - 测试DeepSeek连接
     */
    public boolean checkHealth() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiApiUrl + "/models",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("DeepSeek健康检查失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取AI服务信息
     */
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("provider", "DeepSeek");
        info.put("model", model);
        info.put("max_tokens", maxTokens);
        info.put("temperature", temperature);
        info.put("url", aiApiUrl);
        info.put("health_status", checkHealth() ? "healthy" : "unhealthy");
        return info;
    }

}