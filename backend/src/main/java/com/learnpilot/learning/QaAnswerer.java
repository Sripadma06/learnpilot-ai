package com.learnpilot.learning;

import java.util.ArrayList;
import java.util.List;

/** Answers a student's question by finding the closest question already answered in the material. */
public final class QaAnswerer {
    public record Match(QaPair pair, double score) {}

    private QaAnswerer() {}

    public static List<Match> best(String question, List<QaPair> pairs, int max) {
        if (pairs.isEmpty()) return List.of();
        List<Passage> docs = new ArrayList<>();
        for (QaPair p : pairs) docs.add(new Passage(p.page(), p.question() + " " + p.question() + " " + p.answer()));
        List<PassageRetriever.Hit> hits = PassageRetriever.top(question, docs, 8);
        List<Match> ranked = new ArrayList<>();
        if (hits.isEmpty()) return ranked;
        List<String> terms = Words.tokens(question);
        for (PassageRetriever.Hit h : hits) {
            QaPair p = pairs.get(h.index());
            String q = p.question().toLowerCase(java.util.Locale.ROOT);
            int inQuestion = 0;
            for (String t : new java.util.HashSet<>(terms)) {
                String stem = t.length() > 5 ? t.substring(0, t.length() - 1) : t;
                if (q.contains(stem)) inQuestion++;
            }
            // A match inside the question itself is much stronger evidence than a match in the answer body.
            ranked.add(new Match(p, h.score() + 2.0 * inQuestion));
        }
        ranked.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<Match> out = new ArrayList<>();
        double top = ranked.get(0).score();
        for (Match m : ranked) {
            if (out.size() >= max) break;
            if (m.score() >= top * 0.75) out.add(m);
        }
        return out;
    }

    public static String format(List<Match> matches) {
        StringBuilder sb = new StringBuilder();
        for (Match m : matches) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Q: ").append(m.pair().question()).append("\n").append(m.pair().answer());
        }
        return sb.toString();
    }
}
