package com.g07.file.controller;

import com.g07.common.core.R;
import com.g07.file.service.DocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file/doc")
public class DocController {

    @Autowired
    private DocService docService;

    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        // try {
        //     String docId = docService.uploadDocument(file, tenantId, userId);
        //     return R.ok(docId);
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     return R.fail("上传失败: " + e.getMessage());
        // }
        String docId = docService.uploadDocument(file);
        return R.ok(docId);
    }
}