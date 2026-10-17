package com.learnpilot.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyDocumentRepository extends JpaRepository<StudyDocument, UUID> {
    List<StudyDocument> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<StudyDocument> findByIdAndOwnerId(UUID id, UUID ownerId);
}
