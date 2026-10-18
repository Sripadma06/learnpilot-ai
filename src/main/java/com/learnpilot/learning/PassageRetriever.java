package com.learnpilot.learning;

import java.util.*;

/** Small TF-IDF style keyword retriever. Good enough for a few documents; swap for pgvector later. */
public final class PassageRetriever {
    public record Hit(Passage passage, double score, int index) {}

    private PassageRetriever() {}

    public static List<Hit> top(String question, List<Passage> passages, int k) {
        List<String> q = Words.tokens(question);
        if (q.isEmpty() || passages.isEmpty()) return List.of();
        Map<String, Integer> df = new HashMap<>();
        List<Set<String>> sets = new ArrayList<>();
        for (Passage p : passages) {
            Set<String> s = new HashSet<>(Words.tokens(p.text()));
            sets.add(s);
            for (String t : s) df.merge(t, 1, Integer::sum);
        }
        int n = passages.size();
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double score = 0;
            String lower = passages.get(i).text().toLowerCase(Locale.ROOT);
            for (String t : new HashSet<>(q)) {
                boolean has = sets.get(i).contains(t);
                if (!has && t.length() > 5) { // crude stemming: "collections" ~ "collection"
                    String stem = t.substring(0, t.length() - 1);
                    has = lower.contains(stem);
                }
                if (has) score += Math.log(1.0 + (double) n / (1 + df.getOrDefault(t, 0)));
            }
            if (score > 0) hits.add(new Hit(passages.get(i), score, i));
        }
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.size() > k ? new ArrayList<>(hits.subList(0, k)) : hits;
    }

    /** Best sentences from the hits that mention the question terms (used when no LLM is configured). */
    public static List<String> bestSentences(String question, List<Hit> hits, int max) {
        Set<String> q = new HashSet<>(Words.tokens(question));
        record Scored(String s, double score) {}
        List<Scored> all = new ArrayList<>();
        for (Hit h : hits) {
            for (String s : h.passage().text().split("(?<=[.!?])\\s+")) {
                if (s.length() < 25) continue;
                double sc = 0;
                for (String t : Words.tokens(s)) if (q.contains(t)) sc += 1;
                if (sc > 0) all.add(new Scored(s.trim(), sc + h.score() * 0.1));
            }
        }
        all.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<String> out = new ArrayList<>();
        for (Scored s : all) {
            if (out.size() >= max) break;
            if (!out.contains(s.s())) out.add(s.s());
        }
        return out;
    }
}
