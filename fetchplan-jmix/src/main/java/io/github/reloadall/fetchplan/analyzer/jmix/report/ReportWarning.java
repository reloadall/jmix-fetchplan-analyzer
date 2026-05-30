package io.github.reloadall.fetchplan.analyzer.jmix.report;

import java.util.Objects;

public class ReportWarning {

    private final String kind;
    private final String file;
    private final Integer line;
    private final String message;

    public ReportWarning(String kind, String file, Integer line, String message) {
        this.kind = Objects.requireNonNull(kind, "kind is null");
        this.file = file;
        this.line = line;
        this.message = Objects.requireNonNull(message, "message is null");
    }

    public String getKind() {
        return kind;
    }

    public String getFile() {
        return file;
    }

    public Integer getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }
}