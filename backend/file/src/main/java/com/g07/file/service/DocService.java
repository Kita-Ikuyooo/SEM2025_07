package com.g07.file.service;

import com.g07.common.util.UserContext;
import com.g07.file.config.RabbitConfig;
import com.g07.file.entity.Document;
import com.g07.file.mapper.DocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocService {

    private final MinioService minioService;
    private final DocMapper docMapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(rollbackFor = Exception.class)
    public String uploadDocument(MultipartFile file) {
        // 从上下文自动获取当前用户信息
        String tenantId = UserContext.getTenantId();
        String userId = UserContext.getUserId();

        if (tenantId == null) throw new RuntimeException("非法请求：无法获取租户ID");
        if (userId == null) throw new RuntimeException("非法请求：无法获取用户ID");

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1) : "unknown";
        
        // 生成唯一docId
        String docId = UUID.randomUUID().toString().replace("-", "");
        
        // 构造MinIO存储路径(tenant_id/doc_id/filename)
        // 这种结构方便按租户隔离和按文档归档
        String objectName = String.format("%s/%s/%s", tenantId, docId, originalFilename);

        // 上传至MinIO
        minioService.uploadFile(objectName, file);

        // 保存元数据到MySQL
        Document doc = new Document();
        doc.setDocId(docId);
        doc.setTenantId(tenantId);
        doc.setUserId(userId);
        doc.setDocName(originalFilename);
        doc.setFilePath(objectName);
        doc.setFileType(suffix);
        doc.setFileSize(file.getSize());
        // 初始状态为处理中
        doc.setStatus("processing");
        // doc.setUploadTime(LocalDateTime.now());
        docMapper.insert(doc);

        // 发送消息到 RabbitMQ
        // 消息包含docId和filePath，通知ai模块开始解析
        Map<String, String> message = new HashMap<>();
        message.put("docId", docId);
        message.put("filePath", objectName);
        message.put("tenantId", tenantId);
        
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY, message);

        return docId;
    }
}