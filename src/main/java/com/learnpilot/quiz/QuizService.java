package com.learnpilot.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnpilot.ai.LlmClient;
import com.learnpilot.document.DocumentChunk;
import com.learnpilot.document.DocumentChunkRepository;
import com.learnpilot.document.StudyDocument;
import com.learnpilot.document.StudyDocumentRepository;
import com.learnpilot.learning.GeneratedQuestion;
import com.learnpilot.learning.HeuristicQuizGenerator;
import com.learnpilot.learning.Passage;
import com.learnpilot.learning.QaExtractor;
import com.learnpilot.learning.QaQuizGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuizService {
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private static final int MAX_SOURCE_CHARS = 12_000;

    private final StudyDocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final QuizRepository quizzes;
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final LlmClient llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public QuizService(StudyDocumentRepository documents, DocumentChunkRepository chunks, QuizRepository quizzes,
                       QuizQuestionRepository questions, QuizAttemptRepository attempts, LlmClient llm) {
        this.documents = documents; this.chunks = chunks; this.quizzes = quizzes;
        this.questions = questions; this.attempts = attempts; this.llm = llm;
    }

    public record QuestionView(String id, String text, List<String> options, int page) {}
    public record QuizView(String id, String title, String topic, String source, List<QuestionView> questions) {}
    public record QuizSummary(String id, String title, String topic, String source, int questionCount,
                              java.time.Instant createdAt, Integer lastScore, int attempts) {}
    public record QuestionResult(String questionId, int selected, int correctIndex, boolean correct, String explanation) {}
    public record AttemptResult(int score, int correct, int total, List<QuestionResult> results) {}

    @Transactional
    public QuizView generate(UUID owner, UUID documentId, int requested) {
        StudyDocument doc = documents.findByIdAndOwnerId(documentId, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
        int count = Math.max(3, Math.min(requested, 15));
        List<Passage> passages = new ArrayList<>();
        for (DocumentChunk c : chunks.findByDocumentIdAndOwnerIdOrderByPositionAsc(documentId, owner)) {
            passages.add(new Passage(c.getPageNumber(), c.getText()));
        }

        List<GeneratedQuestion> generated = List.of();
        String source = "OFFLINE";
        if (llm.enabled()) {
            try {
                generated = generateWithLlm(passages, count);
                if (generated.size() >= Math.max(2, count / 2)) source = "AI";
                else generated = List.of();
            } catch (Exception e) {
                log.warn("LLM quiz generation failed, using offline generator: {}", e.toString());
            }
        }
        if (generated.isEmpty()) // offline step 1: questions that already appear in the material, answered from it
            generated = QaQuizGenerator.generate(QaExtractor.extract(passages), count);
        if (generated.isEmpty()) // offline step 2: fill-in-the-blank for plain prose
            generated = HeuristicQuizGenerator.generate(passages, count);
        if (generated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "This document doesn't have enough text to build a quiz from.");
        }

        Quiz quiz = quizzes.save(new Quiz(owner, documentId, "Quiz: " + doc.getTitle(), doc.getTitle(), source));
        List<QuizQuestion> rows = new ArrayList<>();
        int pos = 0;
        for (GeneratedQuestion g : generated) {
            rows.add(new QuizQuestion(quiz.getId(), pos++, g.text(), g.options(), g.correctIndex(), g.explanation(), g.page()));
        }
        questions.saveAll(rows);
        return view(quiz, rows);
    }

    private List<GeneratedQuestion> generateWithLlm(List<Passage> passages, int count) throws Exception {
        // Sample passages evenly so long documents are covered end to end.
        StringBuilder material = new StringBuilder();
        int stride = Math.max(1, (int) Math.ceil(passages.stream().mapToInt(p -> p.text().length() + 12).sum() / (double) MAX_SOURCE_CHARS));
        for (int i = 0; i < passages.size() && material.length() < MAX_SOURCE_CHARS; i += stride) {
            Passage p = passages.get(i);
            material.append("[Page ").append(p.page()).append("] ").append(p.text()).append("\n\n");
        }
        String system = "You write multiple-choice quiz questions strictly from the study material you are given. "
            + "Never use outside facts. Output ONLY a JSON array, with no commentary and no code fences.";
        String user = "Create " + count + " multiple-choice questions that test understanding of the material below. "
            + "Each array element must be: {\"question\": string, \"options\": [exactly 4 distinct strings], "
            + "\"correctIndex\": integer 0-3, \"explanation\": string (one sentence citing the material), "
            + "\"page\": integer (page number from the [Page N] marker)}. Make wrong options plausible. "
            + "Vary the position of the correct answer.\n\nMATERIAL:\n" + material;
        return parse(llm.complete(system, user, 4096), count);
    }

    static final ObjectMapper PARSER = new ObjectMapper();

    static List<GeneratedQuestion> parse(String raw, int max) throws Exception {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end <= start) return List.of();
        JsonNode arr = PARSER.readTree(raw.substring(start, end + 1));
        List<GeneratedQuestion> out = new ArrayList<>();
        for (JsonNode n : arr) {
            JsonNode opts = n.path("options");
            int correct = n.path("correctIndex").asInt(-1);
            String text = n.path("question").asText("").trim();
            if (text.isEmpty() || !opts.isArray() || opts.size() != 4 || correct < 0 || correct > 3) continue;
            List<String> options = new ArrayList<>();
            for (JsonNode o : opts) options.add(o.asText("").trim());
            if (options.stream().anyMatch(String::isEmpty) || options.stream().distinct().count() != 4) continue;
            out.add(new GeneratedQuestion(text, options, correct, n.path("explanation").asText(""), n.path("page").asInt(0)));
            if (out.size() >= max) break;
        }
        return out;
    }

    private QuizView view(Quiz quiz, List<QuizQuestion> rows) {
        List<QuestionView> qs = rows.stream()
            .map(q -> new QuestionView(q.getId().toString(), q.getText(), q.getOptions(), q.getSourcePage())).toList();
        return new QuizView(quiz.getId().toString(), quiz.getTitle(), quiz.getTopic(), quiz.getSource(), qs);
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> list(UUID owner) {
        List<QuizAttempt> mine = attempts.findByUserIdOrderBySubmittedAtDesc(owner);
        List<QuizSummary> out = new ArrayList<>();
        for (Quiz q : quizzes.findByOwnerIdOrderByCreatedAtDesc(owner)) {
            List<QuizAttempt> forQuiz = mine.stream().filter(a -> a.getQuizId().equals(q.getId())).toList();
            out.add(new QuizSummary(q.getId().toString(), q.getTitle(), q.getTopic(), q.getSource(),
                (int) questions.countByQuizId(q.getId()), q.getCreatedAt(),
                forQuiz.isEmpty() ? null : forQuiz.get(0).getScore(), forQuiz.size()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public QuizView get(UUID owner, UUID quizId) {
        Quiz quiz = quizzes.findByIdAndOwnerId(quizId, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));
        return view(quiz, questions.findByQuizIdOrderByPositionAsc(quizId));
    }

    @Transactional
    public AttemptResult submit(UUID owner, UUID quizId, List<Integer> answers) {
        Quiz quiz = quizzes.findByIdAndOwnerId(quizId, owner)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found."));
        List<QuizQuestion> qs = questions.findByQuizIdOrderByPositionAsc(quizId);
        if (answers == null || answers.size() != qs.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide one answer per question (" + qs.size() + ").");
        }
        List<QuestionResult> results = new ArrayList<>();
        int correct = 0;
        for (int i = 0; i < qs.size(); i++) {
            QuizQuestion q = qs.get(i);
            int selected = answers.get(i) == null ? -1 : answers.get(i);
            boolean ok = selected == q.getCorrectIndex();
            if (ok) correct++;
            results.add(new QuestionResult(q.getId().toString(), selected, q.getCorrectIndex(), ok, q.getExplanation()));
        }
        QuizAttempt attempt = attempts.save(new QuizAttempt(owner, quizId, quiz.getTopic(), correct, qs.size()));
        return new AttemptResult(attempt.getScore(), correct, qs.size(), results);
    }
}
