package com.g07.file.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * 上传文件到MinIO
     * @param objectName 存储路径 (例如: tenantId/docId/filename.pdf)
     * @param file 文件对象
     */
    public void uploadFile(String objectName, MultipartFile file) {
        try {
            // 检查Bucket是否存在，不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // 上传流
            try (InputStream stream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(stream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO上传失败: " + e.getMessage());
        }
    }
}