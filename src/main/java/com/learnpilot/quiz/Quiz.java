package com.learnpilot.quiz;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learn_quizzes", indexes = @Index(name = "idx_quizzes_owner", columnList = "ownerId"))
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID ownerId;
    private UUID documentId;
    @Column(nullable = false, length = 255) private String title;
    /** Topic label used for mastery tracking (the source document title). */
    @Column(nullable = false, length = 255) private String topic;
    @Column(nullable = false, length = 20) private String source; // AI or OFFLINE
    @Column(nullable = false) private Instant createdAt = Instant.now();

    protected Quiz() {}
    public Quiz(UUID ownerId, UUID documentId, String title, String topic, String source) {
        this.ownerId = ownerId; this.documentId = documentId; this.title = title; this.topic = topic; this.source = source;
    }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
}
