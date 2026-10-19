package xyz.volcanobay.empiric.core.ui;

import java.util.ArrayList;
import java.util.List;

public interface UiFont {

    int width(String text);

    int lineHeight();

    /**
     * The longest prefix of {@code text} that fits in {@code width}, never splitting a surrogate pair.
     */
    default String trim(String text, int width) {
        int end = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (width(text.substring(0, next)) > width) {
                break;
            }
            end = next;
        }
        return text.substring(0, end);
    }

    default List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (line.isEmpty() || width(candidate) <= width) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
