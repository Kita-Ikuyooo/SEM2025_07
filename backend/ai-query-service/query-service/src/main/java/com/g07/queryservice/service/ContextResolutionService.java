package com.g07.queryservice.service;

import com.g07.queryservice.entity.QaRecord;
import com.g07.queryservice.repository.QaRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 上下文解析服务
 * 用于处理多轮对话中的指代词识别和替换
 */
@Service
public class ContextResolutionService {

    @Autowired
    private QaRecordRepository qaRecordRepository;

    // 指代词模式列表
    private static final List<String> REFERENCE_PATTERNS = Arrays.asList(
        "它", "它们", "它的", "它们的",
        "这个", "这些", "那个", "那些",
        "上述", "之前", "刚才", "之前提到的", "刚才说的", "刚才问的",
        "其", "其中", "上述问题", "这个问题", "那个问题",
        "上面", "下面", "之前的内容", "上述内容"
    );

    // 上下文窗口大小（最近N轮对话）
    private static final int CONTEXT_WINDOW = 5;

    /**
     * 解析当前问题，将指代词替换为上下文中的实际内容
     * @param sessionId 会话ID
     * @param currentQuestion 当前问题
     * @return 解析后的问题（已替换指代词）
     */
    public String resolveContext(String sessionId, String currentQuestion) {
        // 获取最近的对话历史
        List<QaRecord> history = getRecentHistory(sessionId, CONTEXT_WINDOW);
        
        if (history.isEmpty()) {
            // 如果没有历史记录，直接返回原问题
            return currentQuestion;
        }

        // 构建上下文摘要
        String contextSummary = buildContextSummary(history);
        
        // 检测并替换指代词
        String resolvedQuestion = replaceReferences(currentQuestion, history, contextSummary);
        
        return resolvedQuestion;
    }

    /**
     * 获取最近的对话历史
     * @param sessionId 会话ID
     * @param limit 返回的记录数限制
     * @return 按时间倒序排列的对话历史
     */
    private List<QaRecord> getRecentHistory(String sessionId, int limit) {
        List<QaRecord> allRecords = qaRecordRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        if (allRecords.size() > limit) {
            return allRecords.subList(0, limit);
        }
        return allRecords;
    }

    /**
     * 构建上下文摘要（提取关键实体和概念）
     * @param history 对话历史
     * @return 上下文摘要字符串
     */
    private String buildContextSummary(List<QaRecord> history) {
        StringBuilder summary = new StringBuilder();
        
        // 从最近的对话中提取关键信息
        for (int i = history.size() - 1; i >= 0; i--) {
            QaRecord record = history.get(i);
            summary.append("问题：").append(record.getQuestion()).append(" ");
            if (record.getAnswer() != null && !record.getAnswer().isEmpty()) {
                // 从答案中提取关键信息（简单提取前100个字符）
                String answerPreview = record.getAnswer().length() > 100 
                    ? record.getAnswer().substring(0, 100) 
                    : record.getAnswer();
                summary.append("答案：").append(answerPreview).append(" ");
            }
        }
        
        return summary.toString();
    }

    /**
     * 替换问题中的指代词
     * @param question 当前问题
     * @param history 对话历史
     * @param contextSummary 上下文摘要
     * @return 替换后的问题
     */
    private String replaceReferences(String question, List<QaRecord> history, String contextSummary) {
        String resolved = question;
        
        // 检测指代词
        if (!containsReference(question)) {
            return resolved; // 没有指代词，直接返回
        }

        // 从历史记录中提取核心主题实体（找到第一个没有指代词的问题中的主题）
        String coreTopic = extractCoreTopic(history);
        
        // 从最近的问题中提取主题（用于"这个问题"等指代）
        String recentQuestionTopic = extractTopicFromQuestion(history.size() > 0 ? history.get(0).getQuestion() : "");
        
        // 从最近的答案中提取关键信息
        String recentAnswerInfo = extractRecentAnswerInfo(history);

        // 替换常见的指代模式
        // "它"、"它们"等指代核心主题实体
        resolved = replacePattern(resolved, "它", coreTopic);
        resolved = replacePattern(resolved, "它们", coreTopic);
        resolved = replacePattern(resolved, "它的", coreTopic + "的");
        resolved = replacePattern(resolved, "它们的", coreTopic + "的");
        
        // "这个"、"这些"等指代核心主题实体
        resolved = replacePattern(resolved, "这个", coreTopic);
        resolved = replacePattern(resolved, "这些", coreTopic);
        resolved = replacePattern(resolved, "那个", coreTopic);
        resolved = replacePattern(resolved, "那些", coreTopic);
        
        // "上述"指代核心主题
        resolved = replacePattern(resolved, "上述", coreTopic);
        
        // "之前提到的"指代最近答案中的内容（如果答案中有相关内容）
        // 如果问题是"之前提到的应用领域"，那么应该保留"应用领域"，只替换"之前提到的"部分
        // 这里先简单处理：如果答案存在，使用答案；否则使用核心主题
        if (recentAnswerInfo != null && !recentAnswerInfo.isEmpty()) {
            // 对于"之前提到的X"，保留X，只替换"之前提到的"
            // 但这里我们先简单替换，后续可以优化
            resolved = replacePattern(resolved, "之前提到的", recentAnswerInfo);
            resolved = replacePattern(resolved, "刚才说的", recentAnswerInfo);
        } else {
            resolved = replacePattern(resolved, "之前提到的", coreTopic);
            resolved = replacePattern(resolved, "刚才说的", coreTopic);
        }
        resolved = replacePattern(resolved, "刚才问的", recentQuestionTopic);
        
        // "上述问题"、"这个问题"指代最近的问题主题
        resolved = replacePattern(resolved, "上述问题", recentQuestionTopic);
        resolved = replacePattern(resolved, "这个问题", recentQuestionTopic);
        resolved = replacePattern(resolved, "那个问题", recentQuestionTopic);
        
        // 处理"其"、"其中"等
        if (resolved.contains("其")) {
            // 根据上下文判断"其"指代的内容
            if (recentAnswerInfo != null && !recentAnswerInfo.isEmpty()) {
                resolved = resolved.replaceAll("其([^中])", recentAnswerInfo + "$1");
            } else {
                resolved = resolved.replaceAll("其([^中])", coreTopic + "$1");
            }
            // 处理"其中"
            resolved = resolved.replace("其中", recentAnswerInfo != null && !recentAnswerInfo.isEmpty() 
                ? recentAnswerInfo + "中" 
                : coreTopic + "中");
        }

        // 处理"之前"、"刚才"等时间指代
        resolved = replacePattern(resolved, "之前的内容", recentAnswerInfo != null && !recentAnswerInfo.isEmpty() ? recentAnswerInfo : coreTopic);
        resolved = replacePattern(resolved, "上述内容", recentAnswerInfo != null && !recentAnswerInfo.isEmpty() ? recentAnswerInfo : coreTopic);
        
        return resolved.trim();
    }

    /**
     * 检测问题中是否包含指代词
     * @param question 问题文本
     * @return 是否包含指代词
     */
    private boolean containsReference(String question) {
        for (String pattern : REFERENCE_PATTERNS) {
            if (question.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 替换指代模式（针对中文优化）
     * @param text 原始文本
     * @param pattern 指代模式
     * @param replacement 替换内容
     * @return 替换后的文本
     */
    private String replacePattern(String text, String pattern, String replacement) {
        if (replacement == null || replacement.isEmpty()) {
            return text;
        }
        // 直接替换，中文中词边界不明显，使用简单替换
        return text.replace(pattern, replacement);
    }

    /**
     * 从历史记录中提取核心主题实体
     * 找到第一个不包含指代词的问题，从中提取主题实体
     * @param history 对话历史（按时间倒序排列，最新的在前）
     * @return 核心主题/实体
     */
    private String extractCoreTopic(List<QaRecord> history) {
        if (history.isEmpty()) {
            return "";
        }
        
        // 从最旧的记录开始查找（即历史列表的末尾），找到第一个不包含指代词的问题
        // 这样可以找到对话中首次提到的核心实体
        for (int i = history.size() - 1; i >= 0; i--) {
            QaRecord record = history.get(i);
            String question = record.getQuestion();
            
            // 如果这个问题不包含指代词，从中提取主题
            if (!containsReference(question)) {
                String topic = extractTopicFromQuestion(question);
                if (!topic.isEmpty()) {
                    return topic;
                }
            }
        }
        
        // 如果所有问题都包含指代词，尝试从第一个问题的答案中提取
        // 或者返回第一个问题的部分内容（去除指代词后）
        QaRecord firstRecord = history.get(history.size() - 1);
        String firstQuestion = firstRecord.getQuestion();
        String topic = extractTopicFromQuestion(firstQuestion);
        
        // 如果提取的主题仍然包含指代词，尝试从答案中提取
        if (topic.isEmpty() || containsReference(topic)) {
            String answer = firstRecord.getAnswer();
            if (answer != null && !answer.isEmpty()) {
                // 简单提取答案的前30个字符作为主题
                topic = answer.length() > 30 ? answer.substring(0, 30) : answer;
            }
        }
        
        return topic.isEmpty() ? (firstQuestion.length() > 50 ? firstQuestion.substring(0, 50) : firstQuestion) : topic;
    }
    
    /**
     * 从单个问题中提取主题实体
     * @param question 问题文本
     * @return 提取的主题实体
     */
    private String extractTopicFromQuestion(String question) {
        if (question == null || question.isEmpty()) {
            return "";
        }
        
        // 去除常见的疑问词和修饰词，提取核心主题
        String topic = question;
        
        // 首先去除开头的疑问词
        topic = topic
            .replaceAll("^什么是", "")
            .replaceAll("^请介绍一下", "")
            .replaceAll("^请解释一下", "")
            .replaceAll("^请说明", "")
            .replaceAll("^告诉我", "")
            .replaceAll("^请告诉我", "")
            .replaceAll("^介绍一下", "")
            .replaceAll("^解释一下", "")
            .trim();
        
        // 去除开头的指代词（如果存在）
        topic = topic
            .replaceAll("^它", "")
            .replaceAll("^这个", "")
            .replaceAll("^那个", "")
            .replaceAll("^这些", "")
            .replaceAll("^那些", "")
            .trim();
        
        // 去除"的"及其后面的内容（如"的应用领域有哪些"）
        // 但只处理第一个"的"，保留主题部分
        int deIndex = topic.indexOf("的");
        if (deIndex > 0) {
            // 如果"的"后面包含疑问词，说明"的"之前的部分是主题
            String afterDe = deIndex + 1 < topic.length() ? topic.substring(deIndex + 1) : "";
            if (afterDe.contains("哪些") || afterDe.contains("什么") || afterDe.contains("怎样") 
                || afterDe.contains("如何") || afterDe.contains("有") || afterDe.contains("是")) {
                topic = topic.substring(0, deIndex);
            }
        }
        
        // 去除标点符号
        topic = topic.replaceAll("[？?。，,]", "").trim();
        
        // 如果主题太长，截取前50个字符
        if (topic.length() > 50) {
            topic = topic.substring(0, 50);
        }
        
        return topic;
    }

    /**
     * 从历史记录中提取最近的答案信息
     * @param history 对话历史
     * @return 最近的答案摘要
     */
    private String extractRecentAnswerInfo(List<QaRecord> history) {
        if (history.isEmpty()) {
            return "";
        }
        
        QaRecord recentRecord = history.get(0);
        String answer = recentRecord.getAnswer();
        
        if (answer == null || answer.isEmpty()) {
            return "";
        }
        
        // 提取答案的前100个字符作为摘要
        if (answer.length() > 100) {
            return answer.substring(0, 100) + "...";
        }
        
        return answer;
    }

    /**
     * 获取对话上下文（用于调试和展示）
     * @param sessionId 会话ID
     * @return 上下文信息
     */
    public Map<String, Object> getContextInfo(String sessionId) {
        List<QaRecord> history = getRecentHistory(sessionId, CONTEXT_WINDOW);
        Map<String, Object> contextInfo = new HashMap<>();
        contextInfo.put("historyCount", history.size());
        contextInfo.put("history", history);
        contextInfo.put("contextSummary", buildContextSummary(history));
        return contextInfo;
    }
}
