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

If root type is omitted, the service resolves the method by root parameter name and stores the source-declared parameter type in the report target.

## Intended Gradle task contract

A Gradle task was not registered in S14 because the root `build.gradle` was already dirty before this task, and adding the task there would mix this runner change with unrelated working-tree changes.

The intended future task contract is:

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

## Current limitations

- The runner returns analyzer output only; it does not compare against declared Jmix fetch plans.
- Evidence fields are still usually empty because source evidence is not yet propagated through the analyzer model.
- Output reflects best-effort static analysis and must not be treated as proof of fetch-plan correctness.
- Gradle/CLI integration remains a follow-up task.