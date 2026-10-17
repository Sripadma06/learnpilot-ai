package com.learnpilot.document;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", indexes = @Index(name = "idx_document_chunks_doc", columnList = "documentId"))
public class DocumentChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID documentId;
    @Column(nullable = false) private UUID ownerId;
    @Column(name = "page_number") private int pageNumber;
    @Column(name = "pos") private int position;
    @Column(nullable = false, columnDefinition = "text") private String text;

    protected DocumentChunk() {}
    public DocumentChunk(UUID documentId, UUID ownerId, int pageNumber, int position, String text) {
        this.documentId = documentId; this.ownerId = ownerId;
        this.pageNumber = pageNumber; this.position = position; this.text = text;
    }
    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public UUID getOwnerId() { return ownerId; }
    public int getPageNumber() { return pageNumber; }
    public int getPosition() { return position; }
    public String getText() { return text; }
}
