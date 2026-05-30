package io.github.reloadall.fetchplan.analyzer.jmix.report;

public class ReportEvidence {

    private final String file;
    private final Integer line;
    private final String expression;
    private final String note;

    public ReportEvidence(String file, Integer line, String expression, String note) {
        this.file = file;
        this.line = line;
        this.expression = expression;
        this.note = note;
    }

    public String getFile() {
        return file;
    }

    public Integer getLine() {
        return line;
    }

    public String getExpression() {
        return expression;
    }

    public String getNote() {
        return note;
    }
}