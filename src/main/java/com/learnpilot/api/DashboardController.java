package com.learnpilot.api;

import com.learnpilot.document.StudyDocument;
import com.learnpilot.document.StudyDocumentRepository;
import com.learnpilot.quiz.QuizAttempt;
import com.learnpilot.quiz.QuizAttemptRepository;
import com.learnpilot.user.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Learner analytics computed from the user's real quiz attempts. */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    public record TopicMastery(String topic, int mastery, int attempts) {}
    public record Recommendation(String topic, String action, String reason, String priority) {}
    public record Overview(int overallMastery, int quizzesCompleted, int studyStreak, int dueTasks) {}

    private final QuizAttemptRepository attempts;
    private final StudyDocumentRepository documents;
    private final CurrentUser currentUser;

    public DashboardController(QuizAttemptRepository attempts, StudyDocumentRepository documents, CurrentUser currentUser) {
        this.attempts = attempts; this.documents = documents; this.currentUser = currentUser;
    }

    /** Recency-weighted average of the latest five attempts per topic (newest counts most). */
    private List<TopicMastery> masteryFor(List<QuizAttempt> newestFirst) {
        Map<String, List<QuizAttempt>> byTopic = new LinkedHashMap<>();
        for (QuizAttempt a : newestFirst) byTopic.computeIfAbsent(a.getTopic(), k -> new ArrayList<>()).add(a);
        List<TopicMastery> out = new ArrayList<>();
        for (var e : byTopic.entrySet()) {
            double sum = 0, weights = 0;
            for (int i = 0; i < Math.min(5, e.getValue().size()); i++) {
                double w = 1.0 / (1 + i);
                sum += e.getValue().get(i).getScore() * w;
                weights += w;
            }
            out.add(new TopicMastery(e.getKey(), (int) Math.round(sum / weights), e.getValue().size()));
        }
        out.sort(Comparator.comparingInt(TopicMastery::mastery));
        return out;
    }

    @GetMapping("/users/me/mastery")
    @Transactional(readOnly = true)
    public List<TopicMastery> mastery(Authentication auth) {
        return masteryFor(attempts.findByUserIdOrderBySubmittedAtDesc(currentUser.id(auth)));
    }

    @GetMapping("/users/me/recommendations")
    @Transactional(readOnly = true)
    public List<Recommendation> recommendations(Authentication auth) {
        UUID owner = currentUser.id(auth);
        List<TopicMastery> mastery = masteryFor(attempts.findByUserIdOrderBySubmittedAtDesc(owner));
        Set<String> practiced = new HashSet<>();
        List<Recommendation> out = new ArrayList<>();
        for (StudyDocument d : documents.findByOwnerIdOrderByCreatedAtDesc(owner)) {
            boolean seen = mastery.stream().anyMatch(m -> m.topic().equals(d.getTitle()));
            if (!seen) out.add(new Recommendation(d.getTitle(), "Generate and take your first quiz",
                "You uploaded this material but haven't practiced it yet", "High"));
        }
        for (TopicMastery m : mastery) { // already weakest first
            if (m.mastery() < 50) out.add(new Recommendation(m.topic(), "Re-read the material, then retake a quiz",
                "Mastery is " + m.mastery() + "% — your weakest area", "High"));
            else if (m.mastery() < 80) out.add(new Recommendation(m.topic(), "Practice another quiz",
                "Mastery is " + m.mastery() + "% — a bit more practice will lock it in", "Medium"));
            else out.add(new Recommendation(m.topic(), "Schedule a retention quiz",
                "Mastery is " + m.mastery() + "% — revisit later to keep it fresh", "Low"));
        }
        return out;
    }

    @GetMapping("/analytics/overview")
    @Transactional(readOnly = true)
    public Overview overview(Authentication auth) {
        UUID owner = currentUser.id(auth);
        List<QuizAttempt> all = attempts.findByUserIdOrderBySubmittedAtDesc(owner);
        List<TopicMastery> mastery = masteryFor(all);
        int overall = mastery.isEmpty() ? 0 : (int) Math.round(mastery.stream().mapToInt(TopicMastery::mastery).average().orElse(0));

        Set<LocalDate> days = new HashSet<>();
        for (QuizAttempt a : all) days.add(a.getSubmittedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        LocalDate day = LocalDate.now();
        if (!days.contains(day)) day = day.minusDays(1);
        int streak = 0;
        while (days.contains(day)) { streak++; day = day.minusDays(1); }

        int due = (int) recommendations(auth).stream().filter(r -> !r.priority().equals("Low")).count();
        return new Overview(overall, all.size(), streak, due);
    }
}
