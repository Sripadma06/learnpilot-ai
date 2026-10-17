package com.learnpilot.document;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "study_documents", indexes = @Index(name = "idx_study_documents_owner", columnList = "ownerId"))
public class StudyDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 30) private String status;
    private int pageCount;
    private int chunkCount;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    protected StudyDocument() {}
    public StudyDocument(UUID ownerId, String title, int pageCount, int chunkCount) {
        this.ownerId = ownerId; this.title = title; this.status = "READY";
        this.pageCount = pageCount; this.chunkCount = chunkCount;
    }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public int getPageCount() { return pageCount; }
    public int getChunkCount() { return chunkCount; }
    public Instant getCreatedAt() { return createdAt; }
}
