package com.g07.queryservice.controller;

import com.g07.queryservice.dto.ChatRequest;
import com.g07.queryservice.dto.ChatResponse;
import com.g07.queryservice.entity.QaRecord;
import com.g07.queryservice.repository.QaRecordRepository;
import com.g07.queryservice.service.ContextResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private QaRecordRepository qaRecordRepository;
    
    @Autowired
    private ContextResolutionService contextResolutionService;

    // 1. 健康检查端点
    @GetMapping("/health")
    public String health() {
        return "问答服务运行正常 | 时间：" + new Date();
    }

    // 2. 测试端点
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "问答服务API已就绪");
        response.put("endpoints", Arrays.asList(
                "POST /api/chat/completions - 问答接口",
                "GET /api/chat/history - 获取历史",
                "GET /api/chat/health - 健康检查"
        ));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // 3. 核心问答接口
    @PostMapping("/completions")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        // 处理sessionId
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session_" + System.currentTimeMillis();
        }

        // 获取原始问题
        String originalQuestion = request.getQuestion();
        
        // 解析上下文，处理指代词
        String resolvedQuestion = contextResolutionService.resolveContext(sessionId, originalQuestion);
        
        // 如果问题被解析修改过，记录原始问题和解析后的问题
        String questionForProcessing = resolvedQuestion;
        if (!originalQuestion.equals(resolvedQuestion)) {
            // 问题被修改，使用解析后的问题进行处理
            // 可以在日志中记录这个变化
            System.out.println("原始问题: " + originalQuestion);
            System.out.println("解析后的问题: " + resolvedQuestion);
        }

        // 模拟AI回答（使用解析后的问题）
        String answer;
        if (questionForProcessing.contains("你好") || questionForProcessing.toLowerCase().contains("hello")) {
            answer = "你好！我是产业知识问答助手，很高兴为您服务！";
        } else {
            answer = "您的问题 \"" + questionForProcessing + "\" 已收到。我正在为您查找相关信息...";
        }

        // 保存到数据库（保存原始问题）
        QaRecord record = new QaRecord();
        record.setSessionId(sessionId);
        record.setQuestion(originalQuestion); // 保存原始问题
        record.setAnswer(answer);
        qaRecordRepository.save(record);

        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);

        // 模拟引用信息
        List<Map<String, Object>> citations = new ArrayList<>();
        Map<String, Object> citation = new HashMap<>();
        citation.put("source", "知识库文档");
        citation.put("content", "相关产业知识内容");
        citation.put("page", 1);
        citations.add(citation);
        response.setCitations(citations);

        return response;
    }

    // 4. 获取历史记录
    @GetMapping("/history")
    public List<QaRecord> getHistory(@RequestParam String sessionId) {
        // 按时间倒序返回历史记录
        return qaRecordRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
    
    // 6. 获取上下文信息（调试用）
    @GetMapping("/context")
    public Map<String, Object> getContext(@RequestParam String sessionId) {
        return contextResolutionService.getContextInfo(sessionId);
    }

    // 5. 简单问答接口（不保存记录）
    @PostMapping("/simple")
    public Map<String, String> simpleChat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        Map<String, String> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", "这是对问题的简单回答：" + question);
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
}