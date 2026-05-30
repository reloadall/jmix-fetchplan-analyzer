# Single-Method Analysis Report Runner

`SingleMethodAnalysisReportService` is the narrow runner/facade for producing an `AnalysisReport` for one Java method.

It reuses the existing analyzer pipeline:

1. `SourceMethodResolver` resolves the target source method.
2. `AstPathEngine` builds `RawTree`.
3. `RawTreeNormalizer` creates canonical `PathTree`.
4. `PathTreeFlattener` produces canonical path strings.
5. `AnalysisReportFactory` creates `AnalysisReport`.
6. `AnalysisReportJsonRenderer` or `AnalysisReportMarkdownRenderer` renders the output.

No fetch-plan comparison is performed by this runner.

## Service API

The service accepts:

- target class name;
- method name;
- root parameter name;
- optional root type;
- output format: `json` or `markdown`.

Required text inputs are trimmed before method resolution, analyzer execution, and report target creation. Null or blank required inputs are rejected.

If root type is omitted, the service resolves the method by root parameter name and stores the source-declared parameter type in the report target. Passing `fetchplan.rootType` as a fully qualified class name is recommended for overloaded methods. Omitting root type may be ambiguous when multiple overloads share the same method name and root parameter name.

## Gradle task

The root project registers `fetchplanAnalyzeMethod` from `fetchplan-jmix/fetchplan-jmix.gradle`.

Use it to render a single-method report:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=... \
  -Pfetchplan.methodName=... \
  -Pfetchplan.rootParam=... \
  -Pfetchplan.rootType=... \
  -Pfetchplan.format=json
```

Required properties:

- `fetchplan.className`
- `fetchplan.methodName`
- `fetchplan.rootParam`

Optional properties:

- `fetchplan.rootType`
- `fetchplan.format`, default `json`, allowed values: `json`, `markdown`
- `fetchplan.output`, optional file path; if absent, print to stdout
- `fetchplan.expectedPaths`, comma-, semicolon-, or newline-separated expected canonical paths
- `fetchplan.expectedPathsFile`, UTF-8 file containing expected canonical paths, one per line; blank lines and lines starting with `#` are ignored
- `fetchplan.compare`, optional boolean; `true` requires `fetchplan.expectedPaths` or `fetchplan.expectedPathsFile`

When `fetchplan.output` is provided, the rendered report is written as UTF-8 and missing parent directories are created. When it is absent, the rendered report is printed to stdout.

Passing `fetchplan.rootType` as a fully qualified class name is recommended for overloaded methods. Omitting root type may be ambiguous when multiple overloads share the same method name and root parameter name.

## Expected-path comparison mode

If `fetchplan.expectedPaths` or `fetchplan.expectedPathsFile` is supplied, the task outputs an expected-path comparison report instead of the plain `AnalysisReport`.

`fetchplan.compare` has strict opt-in validation semantics:

- `fetchplan.compare=true` means expected paths are required and comparison output is produced;
- supplying `fetchplan.expectedPaths` or `fetchplan.expectedPathsFile` automatically enables comparison output;
- `fetchplan.compare=false` does not suppress comparison when expected paths are supplied.

This mode compares analyzer canonical paths against externally supplied expected paths only. It is **not** declared Jmix fetch-plan comparison.

Expected paths from `fetchplan.expectedPaths` and `fetchplan.expectedPathsFile` are combined, trimmed, deduplicated, and rendered deterministically. Blank inline entries are ignored. In files, blank lines and comment lines beginning with `#` are ignored.

The comparison JSON shape is:

```json
{
  "target": { ... },
  "addonCanonicalPaths": [ ... ],
  "expectedPaths": [ ... ],
  "matchedPaths": [ ... ],
  "missingPaths": [ ... ],
  "extraPaths": [ ... ],
  "unsupportedConstructs": [ ... ],
  "warnings": [ ... ],
  "analysisLimits": [ ... ]
}
```

The comparison Markdown output contains these sections:

- Target
- Summary
- Matched Paths
- Missing Paths
- Extra Paths
- Unsupported Constructs
- Warnings
- Analysis Limits

Empty Markdown sections render as `None`.

Example JSON report to stdout:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService \
  -Pfetchplan.methodName=inspectFirstLine \
  -Pfetchplan.rootParam=document \
  -Pfetchplan.rootType=io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document \
  -Pfetchplan.format=json
```

Example Markdown report to stdout:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService \
  -Pfetchplan.methodName=inspectFirstLine \
  -Pfetchplan.rootParam=document \
  -Pfetchplan.rootType=io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document \
  -Pfetchplan.format=markdown
```

Example JSON file output:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService \
  -Pfetchplan.methodName=inspectFirstLine \
  -Pfetchplan.rootParam=document \
  -Pfetchplan.rootType=io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/inspectFirstLine.json
```

Example exact-match comparison to a JSON file:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService \
  -Pfetchplan.methodName=inspectFirstLine \
  -Pfetchplan.rootParam=document \
  -Pfetchplan.rootType=io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document \
  -Pfetchplan.format=json \
  -Pfetchplan.expectedPaths=lines.product.sku,lines.quantity \
  -Pfetchplan.output=build/reports/fetchplan/inspectFirstLine-compare.json
```

Example comparison with a missing expected path in Markdown:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=io.github.reloadall.fetchplan.analyzer.scenario.document.service.DocumentScenarioService \
  -Pfetchplan.methodName=inspectFirstLine \
  -Pfetchplan.rootParam=document \
  -Pfetchplan.rootType=io.github.reloadall.fetchplan.analyzer.scenario.document.entity.Document \
  -Pfetchplan.format=markdown \
  -Pfetchplan.expectedPaths=lines.product.sku,lines.quantity,lines.price.amount
```

In that example, `lines.price.amount` is reported under missing paths.

## Current limitations

- The runner returns analyzer output and optional externally supplied expected-path comparison only; it does not compare against declared Jmix fetch plans.
- Evidence fields are still usually empty because source evidence is not yet propagated through the analyzer model.
- Output reflects best-effort static analysis and must not be treated as proof of fetch-plan correctness.