package com.g07.queryservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "qa_record")
@Data
public class QaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    @Column(length = 2000)
    private String question;

    @Column(length = 5000)
    private String answer;

    private LocalDateTime createdAt = LocalDateTime.now();
}