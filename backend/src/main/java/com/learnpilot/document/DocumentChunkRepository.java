package com.learnpilot.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentIdAndOwnerIdOrderByPositionAsc(UUID documentId, UUID ownerId);
    List<DocumentChunk> findByOwnerIdOrderByDocumentIdAscPositionAsc(UUID ownerId);
}
