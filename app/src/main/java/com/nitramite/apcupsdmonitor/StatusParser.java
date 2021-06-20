package com.nitramite.apcupsdmonitor;

import androidx.core.util.Consumer;

import java.util.Arrays;
import java.util.List;

public class StatusParser {
    public static abstract class ParseField {
        final String label;
        final Consumer<String> setter;
        final List<String> trim;

        public ParseField(String label, Consumer<String> setter, String... trim) {
            this.label = label;
            this.setter = setter;
            this.trim = Arrays.asList(trim);
        }

        public String getLabel() {
            return label;
        }

        public List<String> getTrim() {
            return trim;
        }

        public Consumer<String> getSetter() {
            return setter;
        }

        public boolean matchesLine(String line) {
            return line.contains(label);
        }

        public void parseLine(String line) {
            if (this.matchesLine(line)) {
                String cleaned = this.clean(line);
                this.setter.accept(cleaned);
            }
        }

        private String clean(final String line) {
            String[] split = line.split(": "); // See : and space, important
            String cleaned = split.length > 0 ? split[1] : "";
            for (String s : this.trim) {
                cleaned = cleaned.replace(s, "");
            }
            return cleaned.trim();
        }

    }
}
