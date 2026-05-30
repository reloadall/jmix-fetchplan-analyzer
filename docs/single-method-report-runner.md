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

When `fetchplan.output` is provided, the rendered report is written as UTF-8 and missing parent directories are created. When it is absent, the rendered report is printed to stdout.

Passing `fetchplan.rootType` as a fully qualified class name is recommended for overloaded methods. Omitting root type may be ambiguous when multiple overloads share the same method name and root parameter name.

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

## Current limitations

- The runner returns analyzer output only; it does not compare against declared Jmix fetch plans.
- Evidence fields are still usually empty because source evidence is not yet propagated through the analyzer model.
- Output reflects best-effort static analysis and must not be treated as proof of fetch-plan correctness.