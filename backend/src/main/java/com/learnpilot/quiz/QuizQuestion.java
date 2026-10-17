package com.learnpilot.quiz;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions_v2", indexes = @Index(name = "idx_quiz_questions_quiz", columnList = "quizId"))
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID quizId;
    @Column(name = "pos") private int position;
    @Column(nullable = false, columnDefinition = "text") private String text;
    @Column(nullable = false, length = 1000) private String optionA;
    @Column(nullable = false, length = 1000) private String optionB;
    @Column(nullable = false, length = 1000) private String optionC;
    @Column(nullable = false, length = 1000) private String optionD;
    private int correctIndex;
    @Column(columnDefinition = "text") private String explanation;
    private int sourcePage;

    protected QuizQuestion() {}
    public QuizQuestion(UUID quizId, int position, String text, java.util.List<String> options,
                        int correctIndex, String explanation, int sourcePage) {
        this.quizId = quizId; this.position = position; this.text = text;
        this.optionA = cut(options.get(0)); this.optionB = cut(options.get(1));
        this.optionC = cut(options.get(2)); this.optionD = cut(options.get(3));
        this.correctIndex = correctIndex; this.explanation = explanation; this.sourcePage = sourcePage;
    }
    private static String cut(String s) { return s.length() > 1000 ? s.substring(0, 1000) : s; }

    public UUID getId() { return id; }
    public UUID getQuizId() { return quizId; }
    public int getPosition() { return position; }
    public String getText() { return text; }
    public java.util.List<String> getOptions() { return java.util.List.of(optionA, optionB, optionC, optionD); }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
    public int getSourcePage() { return sourcePage; }
}
