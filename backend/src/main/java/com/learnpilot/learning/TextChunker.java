package com.learnpilot.learning;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits page text into ~900 character passages on LINE boundaries, keeping line breaks so that
 * questions, headings and bullet lists keep their structure.
 */
public final class TextChunker {
    private TextChunker() {}

    public static final int TARGET = 900;

    /** Normalises bullet glyphs and whitespace; returns clean non-empty lines. */
    public static List<String> cleanLines(String raw) {
        List<String> lines = new ArrayList<>();
        if (raw == null) return lines;
        for (String line : raw.replace('\u0000', ' ').split("\\r?\\n")) {
            String t = line.replace(' ', ' ').replaceAll("[ \\t]+", " ").trim();
            if (t.isEmpty()) continue;
            t = t.replaceFirst("^[•●○◦▪■□◆◇➢➤►▶–—*]\\s*", "- ");
            if (t.matches("^\\d{1,3}$")) continue;                       // lone page numbers
            if (t.matches("(?i)^page \\d+( of \\d+)?$")) continue;
            lines.add(t);
        }
        return lines;
    }

    public static List<Passage> chunkPage(int page, String raw) {
        List<Passage> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : cleanLines(raw)) {
            String l = line.length() > TARGET * 2 ? line.substring(0, TARGET * 2) : line;
            if (current.length() + l.length() + 1 > TARGET && current.length() > 0) {
                out.add(new Passage(page, current.toString().trim()));
                current.setLength(0);
            }
            current.append(l).append('\n');
        }
        if (current.toString().trim().length() > 0) out.add(new Passage(page, current.toString().trim()));
        return out;
    }
}
