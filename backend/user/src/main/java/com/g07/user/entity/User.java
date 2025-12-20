package com.g07.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.g07.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String userId;
    private String tenantId;
    private String username;
    private String passwordHash;
    private String email;
    private String role;
    private Boolean isActive;
    
    // @TableField(fill = FieldFill.INSERT)
    // private LocalDateTime createTime;
}