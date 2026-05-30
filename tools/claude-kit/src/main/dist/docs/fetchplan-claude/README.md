# FetchPlan Claude Kit

This is optional developer tooling for projects that use the Jmix FetchPlan Analyzer addon.

It is **not** part of the runtime addon, starter, or application code. It provides Claude skill/agent instructions and supporting documentation that can be unpacked into a target project.

## Installation

Build the kit ZIP in the analyzer repository:

```bash
gradlew.bat claudeKitZip
```

Unpack the ZIP into the target project root. The unpacked project should contain:

PowerShell:

```powershell
Expand-Archive -Path build/distributions/fetchplan-claude-kit-0.1.0.zip -DestinationPath C:\path\to\target-project -Force
```

bash:

```bash
unzip -o build/distributions/fetchplan-claude-kit-0.1.0.zip -d /path/to/target-project
```

Expected unpacked files:

```text
.claude/skills/fetchplan-audit/SKILL.md
.claude/agents/fetchplan-independent-reviewer.md
docs/fetchplan-claude/README.md
docs/fetchplan-claude/commands.md
docs/fetchplan-claude/expected-paths.md
```

## Requirements

The target project must already have the fetchplan addon installed and must provide the Gradle task:

```bash
gradlew.bat fetchplanAnalyzeMethod
```

This kit does not install the addon and does not modify runtime code.

## Compatibility

- The kit expects the target project to provide `fetchplanAnalyzeMethod`.
- Prefer matching the kit version with the addon version used by the target project.
- Current kit version: `0.1.0`.

## Recommended usage

Ask Claude to use the fetchplan audit workflow for a specific service method, root parameter, and root entity type.

Example request:

```text
Use the fetchplan-audit skill to analyze OrderService.calculateTotal.
Root parameter: order
Root type: com.example.app.entity.Order
Write JSON output under build/reports/fetchplan/.
```

Prefer JSON file output:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.json
```

For independent review and comparison, use the `fetchplan-independent-reviewer` agent. It should inspect source code, create an expected paths file, and run comparison mode with `fetchplan.expectedPathsFile`.

## Limitations

- The analyzer is best-effort static analysis, not a formal verifier.
- Unsupported constructs, warnings, and analysis limits require manual review.
- Empty evidence does not prove that no source evidence exists.
- Addon output must not be treated as proof of fetch-plan correctness.
- Dynamic dispatch, repository/query/reload boundaries, reflection, SpEL, string-based JPQL, and generated behavior may require manual review.
