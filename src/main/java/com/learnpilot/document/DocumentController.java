package com.learnpilot.document;

import com.learnpilot.learning.Passage;
import com.learnpilot.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    public record DocumentDto(String id, String title, String status, int pageCount, int chunkCount,
                              Instant createdAt, String message) {}

    private final StudyDocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final PdfTextExtractor extractor;
    private final CurrentUser currentUser;

    public DocumentController(StudyDocumentRepository documents, DocumentChunkRepository chunks,
                              PdfTextExtractor extractor, CurrentUser currentUser) {
        this.documents = documents; this.chunks = chunks; this.extractor = extractor; this.currentUser = currentUser;
    }

    static DocumentDto toDto(StudyDocument d, String message) {
        return new DocumentDto(d.getId().toString(), d.getTitle(), d.getStatus(), d.getPageCount(),
                d.getChunkCount(), d.getCreatedAt(), message);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DocumentDto upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        UUID owner = currentUser.id(authentication);
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty.");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is 10 MB.");
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file."); }
        // Check the real file signature instead of trusting the browser-supplied content type.
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That file is not a valid PDF.");
        }
        PdfTextExtractor.Result result;
        try { result = extractor.extract(bytes); }
        catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Could not read this PDF (it may be encrypted or damaged).");
        }
        if (result.passages().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "No selectable text found. This looks like a scanned PDF; upload a text-based PDF.");
        }
        String name = file.getOriginalFilename() == null ? "uploaded.pdf" : file.getOriginalFilename();
        String title = name.replaceAll("(?i)\\.pdf$", "").replaceAll("[_]+", " ").trim();
        if (title.isEmpty()) title = "Untitled material";
        if (title.length() > 200) title = title.substring(0, 200);

        StudyDocument doc = documents.save(new StudyDocument(owner, title, result.pageCount(), result.passages().size()));
        List<DocumentChunk> rows = new ArrayList<>();
        int pos = 0;
        for (Passage p : result.passages()) rows.add(new DocumentChunk(doc.getId(), owner, p.page(), pos++, p.text()));
        chunks.saveAll(rows);
        return toDto(doc, "Extracted " + result.passages().size() + " passages from " + result.pageCount()
                + " page(s). You can now generate a quiz or ask the tutor about it.");
    }

    @GetMapping
    public List<DocumentDto> list(Authentication authentication) {
        UUID owner = currentUser.id(authentication);
        return documents.findByOwnerIdOrderByCreatedAtDesc(owner).stream().map(d -> toDto(d, null)).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID owner = currentUser.id(authentication);
        StudyDocument doc = documents.findByIdAndOwnerId(id, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
        chunks.deleteAll(chunks.findByDocumentIdAndOwnerIdOrderByPositionAsc(id, owner));
        documents.delete(doc);
    }
}
