# Analysis Report Contract

The analysis report contract is a stable, machine-readable representation of a single-method analyzer run.

It is intended for external tools, including Claude/Codex-assisted review, CI checks, IDE integrations, and snapshot-style tests that compare analyzer output with an independently reviewed expected fetch plan.

## Scope

The current contract is a report layer on top of existing analyzer output. It does not change the analyzer pipeline:

1. AST analysis still builds `RawTree`.
2. `RawTreeNormalizer` still produces canonical `PathTree`.
3. `PathTreeFlattener` still produces canonical path strings.
4. `AnalysisReportFactory` converts those canonical paths and target metadata into `AnalysisReport`.

## Model

The report can represent:

- target class name;
- target method name;
- root parameter name;
- root type when available;
- canonical paths;
- path confidence;
- evidence entries with `file`, `line`, `expression`, and optional `note`;
- unsupported constructs with `kind`, `file`, `line`, `expression`, and `message`;
- warnings with `kind`, `file`, `line`, and `message`;
- analysis limits / flags.

## Renderers

Two renderers are provided:

- `AnalysisReportJsonRenderer` for deterministic JSON suitable for external tooling and assertions.
- `AnalysisReportMarkdownRenderer` for human-readable summaries.

Both renderers sort path-like collections deterministically and do not introduce random IDs. Empty unsupported/warning/limit sections are rendered explicitly as empty arrays in JSON or `None` in Markdown.

## Current limitations

Source evidence is part of the contract but is not yet populated by the analyzer pipeline for canonical paths. For this task, report paths are created with empty evidence lists unless callers provide evidence explicitly.

The report contract should not be interpreted as proof of fetch plan correctness. It reflects the current best-effort analyzer output and preserves uncertainty/unsupported fields for future improvements.