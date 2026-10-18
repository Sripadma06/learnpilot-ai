package com.learnpilot.learning;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline fallback that builds fill-in-the-blank questions from the material itself, so quizzes work
 * without any API key. Questions are always grounded in a real sentence from the PDF.
 */
public final class HeuristicQuizGenerator {
    private HeuristicQuizGenerator() {}

    private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}\\p{Nd}'-]{4,}");

    private record Candidate(String sentence, int page, String answer) {}

    public static List<GeneratedQuestion> generate(List<Passage> passages, int count) {
        // 1. collect candidate sentences
        List<String> sentences = new ArrayList<>();
        List<Integer> pages = new ArrayList<>();
        for (Passage p : passages) {
            for (String s : p.text().replaceAll("(?m)^- ", "").split("(?<=[.!?])\\s+")) {
                String t = s.replaceAll("\\s+", " ").trim();
                int words = t.split("\\s+").length;
                if (words < 8 || words > 35 || t.length() > 260) continue;
                if (!Character.isLetter(t.charAt(0))) continue;
                long letters = t.chars().filter(Character::isLetter).count();
                if (letters < t.length() * 0.7) continue; // skip tables / numeric junk
                sentences.add(t);
                pages.add(p.page());
            }
        }
        if (sentences.isEmpty()) return List.of();

        // 2. document frequency of terms across sentences
        Map<String, Integer> df = new HashMap<>();
        for (String s : sentences) {
            Set<String> seen = new HashSet<>();
            Matcher m = WORD.matcher(s);
            while (m.find()) seen.add(m.group().toLowerCase(Locale.ROOT));
            for (String t : seen) df.merge(t, 1, Integer::sum);
        }

        // 3. pick one answer term per sentence
        List<Candidate> cands = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String s = sentences.get(i);
            String best = null;
            double bestScore = 0;
            Matcher m = WORD.matcher(s);
            while (m.find()) {
                String w = m.group();
                String lw = w.toLowerCase(Locale.ROOT);
                if (Words.STOP.contains(lw)) continue;
                boolean midCap = Character.isUpperCase(w.charAt(0)) && m.start() > 0;
                boolean camel = w.length() > 3 && w.substring(1).chars().anyMatch(Character::isUpperCase);
                boolean weak = lw.endsWith("ly") || lw.endsWith("ing") || lw.endsWith("ed") || lw.contains("-");
                double score = Math.min(w.length(), 12) + (midCap ? 6 : 0) + (camel ? 8 : 0)
                        + (df.get(lw) >= 2 ? 3 : 0) - (df.get(lw) > 6 ? 4 : 0) - (weak ? 8 : 0);
                if (score > bestScore) { bestScore = score; best = w; }
            }
            if (best != null) cands.add(new Candidate(s, pages.get(i), best));
        }

        // 4. spread the choices across the document
        // Prefer strong answers (technical terms) but keep document order.
        List<Candidate> chosen = new ArrayList<>();
        int pickFrom = Math.min(cands.size(), count * 2);
        int want = Math.min(pickFrom, count * 2); // over-pick: some candidates are dropped below
        for (int i = 0; i < want; i++) chosen.add(cands.get((int) ((long) i * cands.size() / want)));

        // 5. build questions with distractors drawn from other answers in the same material
        Random rnd = new Random(31L * cands.size() + passages.size());
        List<GeneratedQuestion> out = new ArrayList<>();
        Set<String> usedAnswers = new HashSet<>();
        for (Candidate c : chosen) {
            if (out.size() >= count) break;
            if (!usedAnswers.add(c.answer().toLowerCase(Locale.ROOT))) continue;
            List<String> pool = new ArrayList<>();
            for (Candidate o : cands) {
                String a = o.answer();
                if (a.equalsIgnoreCase(c.answer()) || pool.stream().anyMatch(x -> x.equalsIgnoreCase(a))) continue;
                if (Character.isUpperCase(a.charAt(0)) != Character.isUpperCase(c.answer().charAt(0))) continue;
                if (c.sentence().toLowerCase(Locale.ROOT).contains(a.toLowerCase(Locale.ROOT))) continue;
                pool.add(a);
            }
            if (pool.size() < 3) { // relax the capitalisation rule before giving up
                for (Candidate o : cands) {
                    String a = o.answer();
                    if (a.equalsIgnoreCase(c.answer()) || pool.stream().anyMatch(x -> x.equalsIgnoreCase(a))) continue;
                    if (c.sentence().toLowerCase(Locale.ROOT).contains(a.toLowerCase(Locale.ROOT))) continue;
                    pool.add(a);
                }
            }
            if (pool.size() < 3) continue;
            pool.sort(Comparator.comparingInt(a -> Math.abs(a.length() - c.answer().length())));
            List<String> near = new ArrayList<>(pool.subList(0, Math.min(pool.size(), 8)));
            Collections.shuffle(near, rnd);
            List<String> options = new ArrayList<>(near.subList(0, 3));
            options.add(c.answer());
            Collections.shuffle(options, rnd);
            String blanked = Pattern.compile("\\b" + Pattern.quote(c.answer()) + "\\b")
                    .matcher(c.sentence()).replaceFirst("_____");
            out.add(new GeneratedQuestion(
                "Fill in the blank: " + blanked,
                options, options.indexOf(c.answer()),
                "From your material (page " + c.page() + "): \"" + c.sentence() + "\"",
                c.page()));
        }
        return out;
    }
}
