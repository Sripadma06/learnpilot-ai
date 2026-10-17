package com.learnpilot.tutor;

import com.learnpilot.ai.LlmClient;
import com.learnpilot.document.DocumentChunk;
import com.learnpilot.document.DocumentChunkRepository;
import com.learnpilot.document.StudyDocument;
import com.learnpilot.document.StudyDocumentRepository;
import com.learnpilot.learning.Passage;
import com.learnpilot.learning.PassageRetriever;
import com.learnpilot.learning.QaAnswerer;
import com.learnpilot.learning.QaExtractor;
import com.learnpilot.learning.QaPair;
import com.learnpilot.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/tutor")
public class TutorController {
    private static final Logger log = LoggerFactory.getLogger(TutorController.class);

    public record AskRequest(@NotBlank @Size(max = 2000) String question, String documentId) {}
    public record Citation(String documentTitle, int pageNumber, String excerpt) {}
    public record AskResponse(String answer, List<Citation> citations, boolean grounded, String mode) {}

    private final DocumentChunkRepository chunks;
    private final StudyDocumentRepository documents;
    private final CurrentUser currentUser;
    private final LlmClient llm;

    public TutorController(DocumentChunkRepository chunks, StudyDocumentRepository documents,
                           CurrentUser currentUser, LlmClient llm) {
        this.chunks = chunks; this.documents = documents; this.currentUser = currentUser; this.llm = llm;
    }

    @PostMapping("/ask")
    @Transactional(readOnly = true)
    public AskResponse ask(@Valid @RequestBody AskRequest request, Authentication auth) {
        UUID owner = currentUser.id(auth);
        Map<UUID, String> titles = new HashMap<>();
        for (StudyDocument d : documents.findByOwnerIdOrderByCreatedAtDesc(owner)) titles.put(d.getId(), d.getTitle());
        if (titles.isEmpty()) {
            return new AskResponse("You haven't uploaded any material yet. Upload a PDF first and I'll answer from it.",
                List.of(), false, "none");
        }
        UUID only = null;
        if (request.documentId() != null && !request.documentId().isBlank()) {
            try { only = UUID.fromString(request.documentId()); } catch (IllegalArgumentException ignored) { }
        }
        List<DocumentChunk> rows = chunks.findByOwnerIdOrderByDocumentIdAscPositionAsc(owner);
        if (only != null) { final UUID f = only; rows = rows.stream().filter(c -> c.getDocumentId().equals(f)).toList(); }
        List<Passage> passages = rows.stream().map(c -> new Passage(c.getPageNumber(), c.getText())).toList();

        List<PassageRetriever.Hit> hits = PassageRetriever.top(request.question(), passages, 4);
        List<Citation> citations = new ArrayList<>();
        for (PassageRetriever.Hit h : hits) {
            DocumentChunk c = rows.get(h.index());
            String flat = c.getText().replaceAll("\\s+", " ").replaceAll("(?<= )- ", "• ");
            String excerpt = flat.length() > 220 ? flat.substring(0, 220) + "…" : flat;
            citations.add(new Citation(titles.getOrDefault(c.getDocumentId(), "Document"), c.getPageNumber(), excerpt));
        }

        if (llm.enabled()) {
            try {
                StringBuilder ctx = new StringBuilder();
                for (int i = 0; i < hits.size(); i++) {
                    DocumentChunk c = rows.get(hits.get(i).index());
                    ctx.append("[").append(i + 1).append("] (").append(titles.get(c.getDocumentId()))
                       .append(", page ").append(c.getPageNumber()).append(")\n").append(c.getText()).append("\n\n");
                }
                String system = "You are LearnPilot, a patient tutor. Answer using ONLY the numbered excerpts from the student's "
                    + "own material and cite them like [1]. If the excerpts do not contain the answer, say so plainly and "
                    + "then offer a short general explanation clearly labelled as not from their material. Be concise.";
                String user = hits.isEmpty()
                    ? "Question: " + request.question() + "\n\n(No relevant excerpts were found in the student's material.)"
                    : "Excerpts:\n" + ctx + "Question: " + request.question();
                return new AskResponse(llm.complete(system, user, 1024), citations, !hits.isEmpty(), "ai");
            } catch (Exception e) {
                log.warn("LLM tutor call failed, using offline answer: {}", e.toString());
            }
        }

        // Offline mode: no model available. First look for a question the material itself already answers.
        Map<UUID, List<Passage>> byDoc = new LinkedHashMap<>();
        for (DocumentChunk c : rows) byDoc.computeIfAbsent(c.getDocumentId(), k -> new ArrayList<>())
            .add(new Passage(c.getPageNumber(), c.getText()));
        List<QaPair> pairs = new ArrayList<>();
        Map<QaPair, String> pairTitle = new HashMap<>();
        for (var e : byDoc.entrySet()) {
            for (QaPair p : QaExtractor.extract(e.getValue())) { pairs.add(p); pairTitle.put(p, titles.getOrDefault(e.getKey(), "Document")); }
        }
        List<QaAnswerer.Match> qa = QaAnswerer.best(request.question(), pairs, 2);
        if (!qa.isEmpty()) {
            List<Citation> qaCitations = new ArrayList<>();
            for (QaAnswerer.Match m : qa) qaCitations.add(new Citation(pairTitle.get(m.pair()), m.pair().page(), m.pair().question()));
            return new AskResponse(QaAnswerer.format(qa)
                + "\n\n(Offline mode: answers are taken from your material. Add an AI key for written explanations.)",
                qaCitations, true, "offline");
        }
        if (hits.isEmpty()) {
            return new AskResponse("I couldn't find anything about that in your uploaded material. Try different keywords, "
                + "or upload the relevant notes.", List.of(), false, "offline");
        }
        List<String> sentences = PassageRetriever.bestSentences(request.question(), hits, 3);
        String body = sentences.isEmpty() ? citations.get(0).excerpt() : String.join(" ", sentences);
        return new AskResponse("From your material: " + body
            + "\n\n(Offline mode: set ANTHROPIC_API_KEY to get written explanations instead of quoted passages.)",
            citations, true, "offline");
    }
}
