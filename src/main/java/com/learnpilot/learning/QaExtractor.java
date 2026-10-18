package com.learnpilot.learning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds "Question? ... answer" structures (interview-prep sheets, FAQs, study guides) in the extracted text.
 * Each pair is the question as written in the material plus the lines that follow it.
 */
public final class QaExtractor {
    private QaExtractor() {}

    private static final Pattern NUMBERED_Q = Pattern.compile("(?:^|\\s)(?:Q(?:uestion)?\\s*)?\\d{1,3}\\s*[.):]\\s+([A-Z][^?]{8,240}\\?)\\s*$");
    private static final Pattern LEADING = Pattern.compile("^(?:-\\s*)?(?:Q(?:uestion)?\\s*\\d*\\s*[.:)]\\s*|\\d{1,3}\\s*[.)]\\s*)");
    private static final Pattern HEADING = Pattern.compile("(?i)^(?:[A-Za-z &/-]{0,40})\\b(?:questions?|section|part|chapter|topics?|overview|summary)\\b[A-Za-z &/:-]{0,20}$");

    private record Line(String text, int page) {}

    public static List<QaPair> extract(List<Passage> passages) {
        List<Line> lines = new ArrayList<>();
        for (Passage p : passages) for (String l : TextChunker.cleanLines(p.text())) lines.add(new Line(l, p.page()));

        List<QaPair> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int i = 0;
        while (i < lines.size()) {
            String[] q = questionAt(lines, i);
            if (q == null) { i++; continue; }
            int consumed = Integer.parseInt(q[1]);
            int j = i + consumed;
            List<String> items = new ArrayList<>();
            while (j < lines.size() && items.size() < 14) {
                String t = lines.get(j).text();
                if (questionAt(lines, j) != null) break;
                if (HEADING.matcher(t).matches()) break;
                if (t.startsWith("- ")) items.add(t.substring(2).trim());
                else if (items.isEmpty()) items.add(t);
                else items.set(items.size() - 1, items.get(items.size() - 1) + " " + t);
                j++;
            }
            String question = q[0];
            String answer = String.join("\n", items.stream().map(s -> items.size() > 1 ? "- " + s : s).toList());
            long letters = answer.chars().filter(Character::isLetter).count();
            if (answer.length() >= 15 && letters >= answer.length() * 0.6 && seen.add(question.toLowerCase(Locale.ROOT))) {
                out.add(new QaPair(question, answer, lines.get(i).page()));
            }
            i = Math.max(j, i + 1);
        }
        return out;
    }

    /** Returns {question, linesConsumed} if a question starts at index i, else null. */
    private static String[] questionAt(List<Line> lines, int i) {
        String raw = lines.get(i).text();
        if (raw.endsWith("?") && raw.length() >= 12) {
            Matcher m = NUMBERED_Q.matcher(raw);
            String q = m.find() ? m.group(1) : LEADING.matcher(raw).replaceFirst("");
            if (q.length() >= 12 && q.length() <= 260 && Character.isLetter(q.charAt(0))) return new String[]{q.trim(), "1"};
        }
        // question wrapped over two lines
        if (i + 1 < lines.size() && LEADING.matcher(raw).find() && !raw.endsWith("?") && !raw.endsWith(".")) {
            String next = lines.get(i + 1).text();
            if (next.endsWith("?") && !next.startsWith("- ")) {
                String q = (LEADING.matcher(raw).replaceFirst("") + " " + next).trim();
                if (q.length() <= 260) return new String[]{q, "2"};
            }
        }
        return null;
    }

    /** Short single-line version of an answer, suitable as a multiple-choice option. */
    public static String condense(String answer) {
        StringBuilder sb = new StringBuilder();
        List<String> items = new ArrayList<>();
        for (String item : answer.split("\n")) {
            String t = (item.startsWith("- ") ? item.substring(2) : item).trim();
            if (!t.isEmpty()) items.add(t);
        }
        List<String> kept = new ArrayList<>(items);
        kept.removeIf(t -> t.endsWith(":") && items.size() > 1); // drop list headers like "Methods:"
        if (kept.isEmpty()) kept = items;
        for (String t : kept) {
            if (sb.length() > 0) {
                if (sb.length() + t.length() > 150) break;
                sb.append("; ");
            }
            sb.append(t.trim());
            if (sb.length() >= 90) break;
        }
        String s = sb.toString().replaceAll("\\s+", " ").trim();
        if (s.length() > 170) {
            int cut = s.lastIndexOf(' ', 167);
            s = s.substring(0, cut > 60 ? cut : 167) + "…";
        }
        return s;
    }
}
