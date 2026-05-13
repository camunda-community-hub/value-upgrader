package io.camunda.valuestransformer.model;


import java.util.ArrayList;
import java.util.List;

/**
 * Collects all entries produced during a transformation run.
 * Equivalent to Go's report.Report struct and report.go functions.
 */

public class TransformReport {

    public enum EntryKind {
        ERROR, WARNING, CHANGE, SKIP
    }

    /**
     * A single item in the transformation report.
     * Equivalent to Go's report.Entry struct.
     */
    public static class Entry {
        private final EntryKind kind;
        private final String ruleType;
        private final String path;
        private final String description;
        private final String detail;

        public Entry(EntryKind entryKind, String ruleType, String path, String description, String detail) {
            this.kind = entryKind;
            this.ruleType = ruleType;
            this.path = path;
            this.description = description;
            this.detail = detail;
        }

        @Override
        public String toString() {
            String desc = (description != null && !description.isEmpty()) ? description : detail;
            StringBuilder sb = new StringBuilder();
            sb.append("  [").append(ruleType).append("]");
            if (path != null && !path.isEmpty()) sb.append(" ").append(path);
            if (desc != null && !desc.isEmpty()) sb.append(" - ").append(desc);
            if (detail != null && !detail.isEmpty() && !detail.equals(desc)) {
                sb.append(" (").append(detail).append(")");
            }
            return sb.toString();
        }

        public EntryKind getKind() {
            return kind;
        }

        public String getRuleType() {
            return ruleType;
        }

        public String getPath() {
            return path;
        }

        public String getDescription() {
            return description;
        }

        public String getDetail() {
            return detail;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void addChange(String ruleType, String path, String description, String detail) {
        entries.add(new Entry(EntryKind.CHANGE, ruleType, path, description, detail));
    }

    public void addWarning(String ruleType, String path, String description, String detail) {
        entries.add(new Entry(EntryKind.WARNING, ruleType, path, description, detail));
    }

    public void addSkip(String ruleType, String path, String description, String detail) {
        entries.add(new Entry(EntryKind.SKIP, ruleType, path, description, detail));
    }

    public void addError(String ruleType, String path, String description, String detail) {
        entries.add(new Entry(EntryKind.ERROR, ruleType, path, description, detail));
    }

    public boolean hasErrors() {
        return entries.stream().anyMatch(e -> e.getKind() == EntryKind.ERROR);
    }

    public boolean hasWarnings() {
        return entries.stream().anyMatch(e -> e.getKind() == EntryKind.WARNING);
    }

    public long countByKind(EntryKind kind) {
        return entries.stream().filter(e -> e.getKind() == kind).count();
    }

    /**
     * Returns a one-line summary string. Equivalent to Go's Report.Summary().
     */
    public String summary() {
        return String.format("%d changes, %d warnings, %d skipped, %d errors",
                countByKind(EntryKind.CHANGE),
                countByKind(EntryKind.WARNING),
                countByKind(EntryKind.SKIP),
                countByKind(EntryKind.ERROR));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * Returns a human-readable multiline report. Equivalent to Go's Report.Print().
     */
    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Transformation Report ===\n");
        sb.append(String.format("Changes: %d | Warnings: %d | Skipped: %d | Errors: %d\n\n",
                countByKind(EntryKind.CHANGE),
                countByKind(EntryKind.WARNING),
                countByKind(EntryKind.SKIP),
                countByKind(EntryKind.ERROR)));

        appendSection(sb, "Changes:", EntryKind.CHANGE);
        appendSection(sb, "Warnings:", EntryKind.WARNING);
        appendSection(sb, "Skipped:", EntryKind.SKIP);
        appendSection(sb, "Errors:", EntryKind.ERROR);
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, EntryKind kind) {
        List<Entry> section = entries.stream().filter(e -> e.getKind() == kind).toList();
        if (!section.isEmpty()) {
            sb.append(title).append("\n");
            section.forEach(e -> sb.append(e).append("\n"));
            sb.append("\n");
        }
    }


}
