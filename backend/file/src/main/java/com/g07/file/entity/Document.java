package com.g07.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("document")
public class Document {
    @TableId(type = IdType.ASSIGN_UUID)
    private String docId;
    private String tenantId;
    private String userId;
    private String docName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;
    private LocalDateTime processedTime;
}