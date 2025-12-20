package com.g07.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.g07.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String tenantId;
    private String tenantName;
    private String status;
    private Long maxStorage;
    
    // @TableField(fill = FieldFill.INSERT)
    // private LocalDateTime createTime;
}