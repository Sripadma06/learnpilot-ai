package com.learnpilot.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    List<Quiz> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<Quiz> findByIdAndOwnerId(UUID id, UUID ownerId);
}
