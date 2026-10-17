package com.learnpilot.quiz;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts_v2", indexes = @Index(name = "idx_quiz_attempts_user", columnList = "userId"))
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID quizId;
    @Column(nullable = false, length = 255) private String topic;
    private int correctCount;
    private int total;
    private int score; // percent
    @Column(nullable = false) private Instant submittedAt = Instant.now();

    protected QuizAttempt() {}
    public QuizAttempt(UUID userId, UUID quizId, String topic, int correctCount, int total) {
        this.userId = userId; this.quizId = quizId; this.topic = topic;
        this.correctCount = correctCount; this.total = total;
        this.score = total == 0 ? 0 : Math.round(correctCount * 100f / total);
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getQuizId() { return quizId; }
    public String getTopic() { return topic; }
    public int getCorrectCount() { return correctCount; }
    public int getTotal() { return total; }
    public int getScore() { return score; }
    public Instant getSubmittedAt() { return submittedAt; }
}
