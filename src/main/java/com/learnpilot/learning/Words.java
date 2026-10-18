package com.learnpilot.learning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class Words {
    private Words() {}

    static final Set<String> STOP = Set.of(
        "the","and","for","are","but","not","you","all","can","has","had","her","was","one","our","out","day","get",
        "his","how","its","may","new","now","old","see","two","way","who","did","use","that","with","have","this",
        "will","your","from","they","been","were","said","each","which","their","there","would","about","these",
        "other","into","than","then","them","some","what","when","where","while","also","such","only","more",
        "most","over","very","just","like","does","done","being","those","should","could","because","between",
        "through","using","used","uses","make","made","many","much","must","both","after","before","under",
        "again","still","every","first","second","within","without","during","however","therefore","whether",
        "explain","explains","difference","tell","give","define","describe","what's","whats","please");

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String t : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
            if ((t.length() >= 3 || t.matches("\\d+")) && !STOP.contains(t)) out.add(t);
        }
        return out;
    }
}
