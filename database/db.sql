CREATE DATABASE IF NOT EXISTS `g07_qa` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `g07_qa`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/*租户表（Tenant）*/
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `tenant_id` CHAR(32) NOT NULL COMMENT '租户唯一标识(UUID)',
  `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
  `status` ENUM('active', 'suspended') NOT NULL DEFAULT 'active' COMMENT '租户状态',
  `max_storage` BIGINT UNSIGNED NOT NULL DEFAULT 10737418240 COMMENT '最大存储空间',
  `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

/*用户表（User）*/
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` CHAR(32) NOT NULL COMMENT '用户唯一标识',
  `tenant_id` CHAR(32) NOT NULL COMMENT '所属租户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password_hash` CHAR(60) NOT NULL COMMENT 'Bcrypt加密密码',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `role` ENUM('admin', 'researcher', 'viewer') NOT NULL DEFAULT 'viewer' COMMENT '角色',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否激活',
  `last_login` DATETIME(6) NULL COMMENT '最后登录时间',
  `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_tenant` (`tenant_id`),
  CONSTRAINT `fk_user_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

/*文档表（Document）*/
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `doc_id` CHAR(32) NOT NULL COMMENT '文档唯一标识',
  `tenant_id` CHAR(32) NOT NULL COMMENT '所属租户ID',
  `user_id` CHAR(32) NOT NULL COMMENT '上传用户ID',
  `doc_name` VARCHAR(255) NOT NULL COMMENT '文档原始名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT 'MinIO对象存储路径',
  `file_type` VARCHAR(10) NOT NULL COMMENT '文件扩展名',
  `file_size` BIGINT UNSIGNED NOT NULL COMMENT '文件大小',
  `status` ENUM('processing', 'completed', 'failed') NOT NULL DEFAULT 'processing' COMMENT '处理状态',
  `upload_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '上传时间',
  `processed_time` DATETIME(6) NULL COMMENT '处理完成时间',
  PRIMARY KEY (`doc_id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  CONSTRAINT `fk_doc_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`tenant_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_doc_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档元数据表';

/*知识块表（Knowledge Chunk）*/
DROP TABLE IF EXISTS `knowledge_chunk`;
CREATE TABLE `knowledge_chunk` (
  `chunk_id` CHAR(32) NOT NULL COMMENT '知识块唯一标识',
  `tenant_id` CHAR(32) NOT NULL COMMENT '所属租户ID',
  `doc_id` CHAR(32) NOT NULL COMMENT '所属文档ID',
  `content_type` ENUM('text', 'image') NOT NULL COMMENT '内容类型',
  `content` LONGTEXT NOT NULL COMMENT '文本内容或图片描述',
  `vector_id` VARCHAR(64) NOT NULL COMMENT '向量数据库中的ID',
  `page_number` SMALLINT UNSIGNED NULL COMMENT '原文档页码',
  `chunk_index` INT UNSIGNED NOT NULL COMMENT '切片顺序索引',
  `token_count` INT UNSIGNED NULL COMMENT 'Token数量',
  `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`chunk_id`),
  KEY `idx_tenant_doc` (`tenant_id`, `doc_id`),
  UNIQUE KEY `idx_tenant_vector` (`tenant_id`, `vector_id`),
  KEY `idx_tenant_content_type` (`tenant_id`, `content_type`),
  CONSTRAINT `fk_chunk_doc` FOREIGN KEY (`doc_id`) REFERENCES `document` (`doc_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识块表';

/*会话表（Chat Session）*/
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `session_id` CHAR(32) NOT NULL COMMENT '会话ID',
  `tenant_id` CHAR(32) NOT NULL COMMENT '租户ID',
  `user_id` CHAR(32) NOT NULL COMMENT '用户ID',
  `title` VARCHAR(255) NULL COMMENT '会话标题',
  `start_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '开始时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_session` (`user_id`, `start_time`),
  CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

/*问答记录表（Qa Record）*/
DROP TABLE IF EXISTS `qa_record`;
CREATE TABLE `qa_record` (
  `record_id` CHAR(32) NOT NULL COMMENT '问答记录ID',
  `session_id` CHAR(32) NOT NULL COMMENT '所属会话ID',
  `question` TEXT NOT NULL COMMENT '用户问题',
  `answer` TEXT NOT NULL COMMENT 'AI回答',
  `qa_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '问答时间',
  PRIMARY KEY (`record_id`, `qa_time`), 
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答记录表';

/*引用映射表（Citation Mapping）*/
DROP TABLE IF EXISTS `citation_mapping`;
CREATE TABLE `citation_mapping` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `record_id` CHAR(32) NOT NULL COMMENT '问答记录ID',
  `chunk_id` CHAR(32) NOT NULL COMMENT '引用的知识块ID',
  `similarity_score` FLOAT NULL COMMENT '检索相似度得分',
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  KEY `idx_chunk_id` (`chunk_id`),
  CONSTRAINT `fk_citation_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`chunk_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答引用溯源表';

/*审计日志表（Audit Log）*/
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` CHAR(32) NOT NULL COMMENT '租户ID',
  `user_id` CHAR(32) NOT NULL COMMENT '操作者ID',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_resource` VARCHAR(500) NULL COMMENT '目标资源',
  `operation_detail` JSON NULL COMMENT '操作详情',
  `timestamp` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '操作时间戳',
  `status` ENUM('SUCCESS', 'FAILURE') NOT NULL COMMENT '操作状态',
  PRIMARY KEY (`log_id`),
  KEY `idx_tenant_time` (`tenant_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计日志表';

SET FOREIGN_KEY_CHECKS = 1;