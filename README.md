# Jmix FetchPlan Analyzer

Early-stage static analyzer for Jmix/Spring services that extracts used entity paths from Java code and compares them against a declared fetch plan.

## Status

**v1 pilot**
- suitable for internal evaluation
- intended for collecting feedback and unsupported cases
- not a production gatekeeper yet
- best-effort analysis, not a proof of correctness for arbitrary Java code

## Problem

In Jmix/Spring applications, fetch plans tend to drift over time:
- some fields are used in code but missing from the fetch plan
- some fields remain in the fetch plan but are no longer used
- manual verification becomes slow and error-prone

This tool analyzes service methods, extracts used entity paths, normalizes them into canonical paths, and compares them with a declared fetch plan.

## What the tool does

Given:
- a target service class
- a target method
- a root parameter
- a fetch plan

the tool produces:
- raw analysis tree
- normalized canonical paths
- comparison report:
  - Covered
  - Missing
  - Extra
  - Uncertain
  - Declared not confirmed breakdown

## Current capabilities

The current pilot supports:

- linear getter chains
- metadata-backed entity getters
- simple computed entity getter body extraction without emitting fake computed fetch-plan properties
- `if / else`
- `foreach`
- `list.get(0)` as collection element access
- `stream().map(MethodRef)` plus basic pass-through operators
- casts
- alias rebinding
- `UNKNOWN_BREAK`
- interprocedural analysis v1
- recursion guard
- Spring bean resolution for simple interface-based injections

See `docs/SUPPORTED_SCOPE.md` for details and limitations.

## Example output

### Canonical paths

- `type.code`
- `type.name`
- `subcontracts.number`
- `subcontracts.status`
- `subcontracts.date`
- `employee.department.name`
- `employee.email`

### Comparison report

- **Covered** — analyzed paths already covered by fetch plan
- **Missing** — analyzed paths missing in fetch plan
- **Extra** — declared leaf fetch plan paths that are not statically confirmed as used
- **Uncertain** — paths affected by unsupported or partially supported patterns

Interpretation note:

- fetch plans are manual input and are **not** treated as ground truth;
- declared-only paths should be read as **declared but not statically confirmed**, not automatically as analyzer defects;
- the report layer now adds a derived breakdown for declared-not-confirmed paths:
  - declared under uncertainty;
  - possible analyzer gap;
  - probable overfetch;
  - structural/container paths.
- standard Jmix/system leaf fields such as `id` are ignored for fetch-plan comparison and do not count as required
  fetch-plan coverage.
- computed/business getter names themselves are not treated as fetch-plan properties unless metadata-backed;
  simple zero-arg same-entity computed getters may still contribute real backing property reads.

## High-level pipeline

1. Parse Java source and locate target method
2. Build `RawTree`
3. Normalize `RawTree` into canonical `PathTree`
4. Flatten canonical paths
5. Extract declared paths from Jmix fetch plan
6. Compare analyzed vs declared paths
7. Produce report

## Repository structure

- `fetchplan-jmix` — core analyzer addon module
- `fetchplan-jmix-starter` — starter/autoconfiguration module
- `fetchplan-jmix-test-scenarios` — analyzable source-fixture / living-documentation module
- `tools/claude-kit` — optional Claude skill/agent distribution kit; developer tooling only, not runtime addon code
- `docs/ARCHITECTURE.md` — architecture overview
- `docs/SUPPORTED_SCOPE.md` — supported/unsupported patterns
- `docs/ROADMAP.md` — v1 / v1.1 / v2
- `docs/DECISIONS.md` — important design decisions
- `docs/EXAMPLES.md` — sample inputs and outputs
- `docs/FEEDBACK_TEMPLATE.md` — how to report unsupported cases
- `docs/single-method-report-runner.md` — single-method AnalysisReport runner usage
- `tools/claude-kit/README.md` — optional Claude kit packaging and installation
- `tools/claude-kit/src/main/dist/docs/fetchplan-claude/README.md` — documentation shipped inside the Claude kit

## Scenario fixture module

The repository includes `fetchplan-jmix-test-scenarios`, a dedicated source-fixture module.

- It uses `src/main/java` intentionally because the analyzer scans it as regular source code.
- The code in this module is **not production application logic**; it is analyzable fixture code for regression and integration tests.
- The current document-oriented scenario now includes one baseline root flow plus multiple focused root methods for isolated analyzer capabilities.
- The baseline root remains `DocumentScenarioService.inspectDocument(Document document)`.
- Additional focused roots cover branch flow, collection `get(0)`, explicit `this` call, value-call assignment, local alias rebinding, alias chain, cast-based continuation, narrow chained `stream().map(MethodRef)`, and one uncertainty case.
- Focused engine regression coverage also includes computed getter handling: fake computed paths are suppressed, while simple
  computed getter bodies can recover real backing property reads such as `getCodeAsEnum() -> code`.

Current limitation:

- the scenario currently stores fetch-plan expectations as a plain path-set fixture rather than a production-like Jmix `FetchPlan` object;
- this keeps the scenario integration test small and avoids broad analyzer changes at this stage.
- object-reference terminal usage without deeper property access still has an explicit open policy question; see `docs/ISSUES_TRACKER.md` (`ISSUE-013`).
- report-layer interpretation now distinguishes between confirmed usage and declared-but-not-confirmed paths more explicitly;
- comparison also ignores standard system/default leaf fields (for example `id`) so they do not produce misleading report noise.

Current test status:

- focused scenario and interprocedural regression tests are in place;
- the full `fetchplan-jmix` test task is currently green, including the legacy Spring context smoke test.

## Intended usage

Current intended usage is **manual/internal analysis**:
- run analysis for a real service method
- inspect normalized paths
- compare with fetch plan
- review `Missing`, `Extra`, and `Uncertain`
- collect unsupported patterns for future iterations

### Single-method report runner

For a focused method-level report, use the `fetchplanAnalyzeMethod` Gradle task:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.json
```

Notes:

- prefer `fetchplan.output` for JSON because stdout may contain Gradle or logging warnings;
- pass `fetchplan.rootType` as a fully qualified class name when known;
- output is best-effort static analysis, not proof of fetch-plan correctness.

### Expected-path comparison

The runner can compare addon canonical paths against externally supplied expected paths:

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

This is expected-path comparison, not declared Jmix FetchPlan comparison. Expected paths are review inputs, not authoritative truth.

### Optional Claude kit

Build the optional Claude skill/agent distribution kit with:

```bash
gradlew.bat claudeKitZip
```

Output:

```text
build/distributions/fetchplan-claude-kit-0.1.0.zip
```

Unpack the ZIP into a target project root. It creates `.claude/...` and `docs/fetchplan-claude/...` files for Claude-assisted review workflows.

The target project must already provide `fetchplanAnalyzeMethod`. The Claude kit is optional developer tooling and is not runtime addon code.

#### Claude-assisted workflows

The kit can help with day-to-day project review and with collecting feedback for improving the analyzer:

- `fetchplan-audit` runs the addon and explains canonical paths, unsupported patterns, warnings, and limits.
- `fetchplan-independent-reviewer` independently inspects source code, prepares expected paths, runs comparison mode, and explains matched, missing, and extra paths.
- Useful feedback artifacts can include addon JSON, an expected paths file, comparison JSON, and short notes about surprising or unsupported cases.

Claude output is review assistance, not proof of fetch-plan correctness. See `tools/claude-kit/README.md` and `tools/claude-kit/src/main/dist/docs/fetchplan-claude/README.md` for details.

## Non-goals for current version

This project does **not** try to:
- fully understand arbitrary Java code
- act as a mathematically complete verifier
- replace human review in all cases
- support reflection-heavy or runtime-generated logic

## Contributing / feedback

If you want to help, please use the feedback format in `docs/FEEDBACK_TEMPLATE.md`.

## License
- Apache-2.0