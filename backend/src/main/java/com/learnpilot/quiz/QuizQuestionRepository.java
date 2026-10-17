package com.learnpilot.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByQuizIdOrderByPositionAsc(UUID quizId);
    long countByQuizId(UUID quizId);
}
