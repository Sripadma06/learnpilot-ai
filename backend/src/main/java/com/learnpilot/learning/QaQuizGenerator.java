package com.learnpilot.learning;

import java.util.*;

/** Turns the material's own question/answer pairs into multiple-choice questions. */
public final class QaQuizGenerator {
    private QaQuizGenerator() {}

    public static List<GeneratedQuestion> generate(List<QaPair> pairs, int count) {
        List<QaPair> usable = new ArrayList<>();
        for (QaPair p : pairs) if (QaExtractor.condense(p.answer()).length() >= 12) usable.add(p);
        if (usable.size() < 4) return List.of(); // need three other answers as distractors

        int want = Math.min(count, usable.size());
        Random rnd = new Random(17L * usable.size() + pairs.size());
        List<GeneratedQuestion> out = new ArrayList<>();
        for (int k = 0; k < want; k++) {
            QaPair p = usable.get((int) ((long) k * usable.size() / want));
            String correct = QaExtractor.condense(p.answer());
            List<String> pool = new ArrayList<>();
            for (QaPair o : usable) {
                if (o == p) continue;
                String c = QaExtractor.condense(o.answer());
                if (c.equalsIgnoreCase(correct) || pool.contains(c)) continue;
                pool.add(c);
            }
            if (pool.size() < 3) continue;
            pool.sort(Comparator.comparingInt(a -> Math.abs(a.length() - correct.length())));
            List<String> near = new ArrayList<>(pool.subList(0, Math.min(pool.size(), 9)));
            Collections.shuffle(near, rnd);
            List<String> options = new ArrayList<>(near.subList(0, 3));
            options.add(correct);
            Collections.shuffle(options, rnd);
            out.add(new GeneratedQuestion(p.question(), options, options.indexOf(correct),
                "From your material (page " + p.page() + "):\n" + p.answer(), p.page()));
        }
        return out;
    }
}
