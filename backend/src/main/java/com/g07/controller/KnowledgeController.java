package com.g07.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.g07.common.R;
import com.g07.entity.AuditLog;
import com.g07.entity.Category;
import com.g07.entity.Document;
import com.g07.entity.KnowledgeChunk;
import com.g07.entity.User;
import com.g07.mapper.AuditLogMapper;
import com.g07.mapper.CategoryMapper;
import com.g07.mapper.DocumentMapper;
import com.g07.mapper.KnowledgeChunkMapper;
import com.g07.mapper.UserMapper;
import com.g07.service.FileService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/docs")
@CrossOrigin
public class KnowledgeController {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private KnowledgeChunkMapper chunkMapper;

    @Autowired
    private FileService fileService;

    @Value("${minio.bucketName}")
    private String bucketName;

    private void recordLog(String tenantId, String userId, String username, String type, String target, String resourceId, String detail) {
        try {
            AuditLog log = new AuditLog();
            log.setTenantId(tenantId != null ? tenantId : "unknown_tenant");
            log.setUserId(userId != null ? userId : "unknown_user");
            log.setUsername(username != null ? username : "unknown"); 
            log.setOperationType(type);
            
            if (target != null && target.length() > 255) target = target.substring(0, 250) + "...";
            log.setTargetResource(target);
            log.setResourceId(resourceId);
            
            log.setOperationDetail(detail);
            log.setTimestamp(LocalDateTime.now());
            log.setStatus("SUCCESS");
            
            auditLogMapper.insert(log);
        } catch (Exception e) {
            System.err.println(">>> 审计日志写入异常: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    // ==================== 文件夹 (Category) 管理 ====================

    @PostMapping("/category/add")
    public R<String> addCategory(@RequestBody Map<String, String> body, 
                                 @RequestHeader("X-Tenant-Id") String tenantId,
                                 @RequestHeader("X-User-Id") String userId) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) return R.error("文件夹名称不能为空");

        String catId = UUID.randomUUID().toString().replace("-", "");

        Category cat = new Category();
        cat.setId(catId);
        cat.setName(name);
        cat.setTenantId(tenantId);
        cat.setCreateTime(LocalDateTime.now());

        categoryMapper.insert(cat);

        User u = userMapper.selectById(userId);
        if (u == null || "viewer".equals(u.getRole())) {
            return R.error("权限不足");
        }
        String username = (u != null) ? u.getUsername() : "unknown";
        recordLog(tenantId, userId, username, "CREATE_FOLDER", name, catId, "新建知识库文件夹");

        return R.ok("知识库文件夹创建成功");
    }

    @GetMapping("/category/list")
    public R<List<Category>> listCategories(@RequestHeader("X-Tenant-Id") String tenantId,
                                            @RequestHeader("X-User-Id") String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return R.error("用户不存在");

        List<Category> list;
        if ("admin".equals(user.getRole())) {
            list = categoryMapper.selectAllWithTenantName();
        } else {
            QueryWrapper<Category> query = new QueryWrapper<>();
            query.eq("tenant_id", tenantId);
            // 【修改】优先按 sort_order 升序，其次按时间
            query.orderByAsc("sort_order").orderByDesc("create_time");
            list = categoryMapper.selectList(query);
        }
        return R.ok(list);
    }

    @PostMapping("/category/rename")
    public R<String> renameCategory(@RequestBody Map<String, String> body,
                                    @RequestHeader("X-User-Id") String userId) {
        // 权限校验
        User u = userMapper.selectById(userId);
        if (u == null || "viewer".equals(u.getRole())) {
            return R.error("权限不足");
        }

        String id = body.get("id");
        String newName = body.get("name");
        
        if (id == null || newName == null) return R.error("参数错误");
        
        Category cat = categoryMapper.selectById(id);
        if (cat == null) return R.error("文件夹不存在");

        cat.setName(newName);
        categoryMapper.updateById(cat);
        
        // 记录简单的审计日志(复用你之前的 recordLog 逻辑，此处简略)
        // recordLog(..., "RENAME_FOLDER", newName, ...);
        
        return R.ok("重命名成功");
    }

    @PostMapping("/category/reorder")
    public R<String> reorderCategories(@RequestBody List<String> sortedIds,
                                       @RequestHeader("X-User-Id") String userId) {
        // 权限校验
        User u = userMapper.selectById(userId);
        if (u == null || "viewer".equals(u.getRole())) {
            return R.error("权限不足");
        }

        if (sortedIds == null || sortedIds.isEmpty()) return R.ok("无变化");

        // 简单的循环更新（数量不大时性能可接受）
        for (int i = 0; i < sortedIds.size(); i++) {
            String catId = sortedIds.get(i);
            Category cat = new Category();
            cat.setId(catId);
            cat.setSortOrder(i); // 索引即权重，0, 1, 2...
            categoryMapper.updateById(cat);
        }

        return R.ok("顺序已保存");
    }

    @DeleteMapping("/category/{id}")
    public R<String> deleteCategory(@PathVariable String id,
                                    @RequestHeader("X-User-Id") String userId) {
        Long count = documentMapper.selectCount(new QueryWrapper<Document>().eq("category_id", id));
        if (count > 0) return R.error("文件夹内尚有文件，无法删除");

        Category cat = categoryMapper.selectById(id);
        if (cat == null) return R.error("文件夹不存在");

        categoryMapper.deleteById(id);

        User u = userMapper.selectById(userId);
        if (u == null || "viewer".equals(u.getRole())) {
            return R.error("权限不足");
        }
        String username = (u != null) ? u.getUsername() : "unknown";
        String tenantId = cat.getTenantId();
        
        recordLog(tenantId, userId, username, "DELETE_FOLDER", cat.getName(), id, "删除文件夹");

        return R.ok("文件夹已删除");
    }

    // ==================== 文件 (Document) 管理 ====================

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam("categoryId") String categoryId,
                            HttpServletRequest request) {
        if (file.isEmpty()) return R.error("文件不能为空");

        String tenantId = request.getHeader("X-Tenant-Id");
        String userId = request.getHeader("X-User-Id");

        if (tenantId == null || userId == null) return R.error("未获取到有效的用户信息");

        try {
            User u = userMapper.selectById(userId);
            if (u == null || "viewer".equals(u.getRole())) {
                return R.error("权限不足");
            }

            String username = (u != null) ? u.getUsername() : "unknown";

            Category cat = categoryMapper.selectById(categoryId);
            String folderName = (cat != null) ? cat.getName() : "未知文件夹";

            String originalName = file.getOriginalFilename();
            String docId = UUID.randomUUID().toString().replace("-", "");
            String fileType = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".") + 1) : "unknown";
            String objectName = tenantId + "/" + categoryId + "/" + docId + "_" + originalName;

            // 1. MinIO 上传
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 2. Document 表插入
            Document doc = new Document();
            doc.setDocId(docId);
            doc.setTenantId(tenantId);
            doc.setUserId(userId);
            doc.setCategoryId(categoryId);
            doc.setDocName(originalName);
            doc.setFilePath(objectName);
            doc.setFileType(fileType);
            doc.setFileSize(file.getSize());
            doc.setStatus("completed");
            doc.setUploadTime(LocalDateTime.now());
            documentMapper.insert(doc);

            // ================== 文件内容解析与入库逻辑 ==================
            String lowerType = fileType.toLowerCase();
            if (lowerType.equals("pdf") || lowerType.equals("doc") || lowerType.equals("docx") || lowerType.equals("txt")) {
                try {
                    System.out.println(">>> [FileService] 开始解析文件: " + originalName);
                    
                    String fullText = fileService.extractTextFromMinio(objectName);
                    
                    if (fullText != null && !fullText.trim().isEmpty()) {
                        List<String> textChunks = fileService.splitTextIntoChunks(fullText, 800, 100);
                        
                        int index = 0;
                        for (String content : textChunks) {
                            KnowledgeChunk chunk = new KnowledgeChunk();
                            chunk.setChunkId(UUID.randomUUID().toString().replace("-", ""));
                            chunk.setTenantId(tenantId);
                            chunk.setDocId(docId);
                            chunk.setContentType("text");
                            chunk.setContent(content);
                            chunk.setChunkIndex(index++);
                            
                            // ==========================================
                            // 【核心修复】生成随机 UUID 作为 vectorId，确保唯一性
                            // ==========================================
                            chunk.setVectorId(UUID.randomUUID().toString().replace("-", "")); 
                            
                            chunk.setCreateTime(LocalDateTime.now());
                            
                            chunkMapper.insert(chunk);
                        }
                        System.out.println(">>> [FileService] 解析完成，共生成 " + index + " 个知识切片");
                    } else {
                        System.out.println(">>> [FileService] 警告：文件解析结果为空");
                    }
                } catch (Exception ex) {
                    System.err.println(">>> [FileService] 解析过程异常: " + ex.getMessage());
                }
            }
            // ==============================================================

            recordLog(tenantId, userId, username, "UPLOAD", originalName, docId, "上传文件至文件夹: " + folderName);

            return R.ok("文件上传成功");

        } catch (Exception e) {
            e.printStackTrace();
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public R<List<Document>> list(@RequestParam("categoryId") String categoryId) {
        QueryWrapper<Document> query = new QueryWrapper<>();
        query.eq("category_id", categoryId).orderByDesc("upload_time");
        return R.ok(documentMapper.selectList(query));
    }

    @GetMapping("/url/{type}/{docId}")
    public R<String> getFileUrl(@PathVariable String docId,
                                @PathVariable String type,
                                @RequestHeader(value = "X-User-Id", required = false) String userId,
                                @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        Document doc = documentMapper.selectById(docId);
        if (doc == null) return R.error("文档不存在");

        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(doc.getFilePath())
                    .expiry(2, TimeUnit.HOURS);

            if ("download".equals(type)) {
                Map<String, String> reqParams = new HashMap<>();
                reqParams.put("response-content-disposition", "attachment; filename=\"" + doc.getDocName() + "\"");
                builder.extraQueryParams(reqParams);
            }

            String url = minioClient.getPresignedObjectUrl(builder.build());

            if (userId != null) {
                User u = userMapper.selectById(userId);
                String username = (u != null) ? u.getUsername() : "unknown";
                String tId = (tenantId != null) ? tenantId : doc.getTenantId();
                String action = "download".equals(type) ? "DOWNLOAD" : "PREVIEW";
                
                recordLog(tId, userId, username, action, doc.getDocName(), docId, "获取链接: " + type);
            }

            return R.ok(url);
        } catch (Exception e) {
            return R.error("生成链接失败");
        }
    }

    @DeleteMapping("/{docId}")
    public R<String> delete(@PathVariable String docId,
                            @RequestHeader("X-User-Id") String userId) {
        User operator = userMapper.selectById(userId);
        if (operator == null) return R.error("用户不存在");
        if (!"admin".equals(operator.getRole())) return R.error("权限不足");

        Document doc = documentMapper.selectById(docId);
        if (doc != null) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName).object(doc.getFilePath()).build());
                
                documentMapper.deleteById(docId);

                chunkMapper.delete(new QueryWrapper<KnowledgeChunk>().eq("doc_id", docId));

                String tId = operator.getTenantId();
                if (tId == null) tId = doc.getTenantId();
                
                recordLog(tId, userId, operator.getUsername(), "DELETE", doc.getDocName(), docId, "管理员删除文件");

                return R.ok("文档已删除");
            } catch (Exception e) {
                return R.error("删除失败");
            }
        }
        return R.error("记录不存在");
    }
}