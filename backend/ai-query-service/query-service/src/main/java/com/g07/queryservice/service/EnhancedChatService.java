// EnhancedChatService.java
package com.g07.queryservice.service;

import com.g07.queryservice.dto.ChatRequest;
import com.g07.queryservice.dto.ChatResponse;
import com.g07.queryservice.entity.QaRecord;
import com.g07.queryservice.repository.QaRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EnhancedChatService {

    @Autowired
    private QaRecordRepository qaRecordRepository;

    @Autowired
    private ContextResolutionService contextResolutionService;

    @Autowired
    private AIService aiService;

    /**
     * AI增强的问答接口
     */
    public ChatResponse aiEnhancedChat(ChatRequest request) {
        // 1. 复用原有的上下文解析
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "session_" + System.currentTimeMillis();
        }

        String originalQuestion = request.getQuestion();
        String resolvedQuestion = contextResolutionService.resolveContext(sessionId, originalQuestion);

        // 2. 检索相关上下文（AI增强部分）
        List<String> relevantContext = aiService.retrieveRelevantContext(resolvedQuestion);

        // 3. 获取AI回答 - 使用正确的方法名
        String aiAnswer = aiService.getDeepSeekResponse(resolvedQuestion, relevantContext);

        // 4. 如果AI回答为空，使用原有的简单逻辑
        String finalAnswer;
        if (aiAnswer != null && !aiAnswer.isEmpty()) {
            finalAnswer = aiAnswer;
        } else {
            // 回退到原有逻辑
            finalAnswer = generateFallbackAnswer(resolvedQuestion);
        }

        // 5. 保存记录（复用原有逻辑）
        QaRecord record = new QaRecord();
        record.setSessionId(sessionId);
        record.setQuestion(originalQuestion);
        record.setAnswer(finalAnswer);
        qaRecordRepository.save(record);

        // 6. 构建响应
        ChatResponse response = new ChatResponse();
        response.setAnswer(finalAnswer);
        response.setSessionId(sessionId);

        // 7. 添加引用信息
        response.setCitations(generateCitations(relevantContext));

        return response;
    }

    private String generateFallbackAnswer(String question) {
        // 原有的简单回答逻辑
        if (question.contains("你好") || question.toLowerCase().contains("hello")) {
            return "你好！我是产业知识问答助手，很高兴为您服务！";
        } else {
            return "您的问题 \"" + question + "\" 已收到。我正在为您查找相关信息...";
        }
    }

    private List<Map<String, Object>> generateCitations(List<String> context) {
        List<Map<String, Object>> citations = new ArrayList<>();

        if (context != null && !context.isEmpty()) {
            for (int i = 0; i < Math.min(context.size(), 3); i++) {
                Map<String, Object> citation = new HashMap<>();
                citation.put("source", "知识库文档");
                citation.put("content", context.get(i));
                citation.put("relevance", (i + 1));
                citations.add(citation);
            }
        } else {
            // 默认引用
            Map<String, Object> citation = new HashMap<>();
            citation.put("source", "知识库文档");
            citation.put("content", "相关产业知识内容");
            citations.add(citation);
        }

        return citations;
    }

    /**
     * 简单的AI问答（不保存记录）
     */
    public ChatResponse simpleAIChat(ChatRequest request) {
        String question = request.getQuestion();
        List<String> context = aiService.retrieveRelevantContext(question);
        String aiAnswer = aiService.getDeepSeekResponse(question, context);

        if (aiAnswer == null || aiAnswer.isEmpty()) {
            aiAnswer = "抱歉，AI服务暂时无法回答这个问题。";
        }

        ChatResponse response = new ChatResponse();
        response.setAnswer(aiAnswer);
        response.setSessionId(request.getSessionId());
        response.setCitations(generateCitations(context));

        return response;
    }
}