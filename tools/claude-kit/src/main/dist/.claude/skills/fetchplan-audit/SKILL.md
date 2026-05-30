---
name: fetchplan-audit
description: Use the Jmix FetchPlan Analyzer addon to inspect Java/Jmix service methods, compare best-effort canonical paths with expected paths, and discuss possible fetch plan incompleteness without claiming proof of correctness.
---

# FetchPlan Audit Skill

Use this workflow when the user asks to:

- analyze a Java/Jmix service method with the fetchplan addon;
- understand what the addon sees or misses;
- compare addon output with expected entity paths;
- investigate possible fetch plan incompleteness.

## Core principles

- Prefer JSON file output over stdout because Gradle, SLF4J, or plugin warnings may pollute console output.
- Always pass `rootType` as a fully qualified class name when it is known.
- Treat addon `canonicalPaths` as best-effort static analysis, not proof.
- Treat `unsupportedConstructs`, `warnings`, and `analysisLimits` as important review inputs.
- Do not treat empty evidence as proof that no source evidence exists.
- For comparison, prefer `fetchplan.expectedPathsFile` over long inline `fetchplan.expectedPaths`.
- Never claim addon output proves fetch-plan correctness.

## Plain JSON report

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.json
```

Read the JSON file and review at least:

- target metadata;
- `canonicalPaths`;
- expected-path comparison sections, when `fetchplan.expectedPaths` or `fetchplan.expectedPathsFile` was supplied;
- `unsupportedConstructs`;
- `warnings`;
- `analysisLimits`.

## Comparison with expected paths file

Create a UTF-8 expected paths file, one path per line:

```text
# build/reports/fetchplan/order-calculate-total.expected-paths.txt
customer.name
lines.product.name
```

Then run:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.expectedPathsFile=build/reports/fetchplan/order-calculate-total.expected-paths.txt \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total-comparison.json
```

## Reporting guidance

When summarizing results, clearly separate:

- addon canonical paths;
- expected paths supplied by the reviewer or user;
- matched paths;
- paths missing in addon output;
- paths extra in addon output;
- unsupported constructs, warnings, and limits;
- manual review notes and uncertainty.

Use cautious language. The addon is an early-stage static analyzer and can miss source behavior or report uncertain areas.
