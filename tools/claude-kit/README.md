# FetchPlan Claude Kit

This directory contains an optional companion Claude kit for projects that use the Jmix FetchPlan Analyzer addon.

It is developer tooling only. It is **not** runtime addon code, is not a Jmix module, and should not be added to `settings.gradle` as an addon subproject.

## Contents

The distributable files live under:

```text
tools/claude-kit/src/main/dist
```

The ZIP is intended to be unpacked into the root of another project that already uses the fetchplan addon.

## Build the ZIP

From the repository root:

```bash
gradlew.bat claudeKitZip
```

Expected output:

```text
build/distributions/fetchplan-claude-kit-<version>.zip
```

## Compatibility

- The kit expects the target project to provide `fetchplanAnalyzeMethod`.
- Prefer matching the kit version with the addon version used by the target project.
- Current kit version: `0.1.0`.

## Install in a target project

Unpack the ZIP into the target project root so it creates:

```text
.claude/
docs/fetchplan-claude/
```

PowerShell:

```powershell
Expand-Archive -Path build/distributions/fetchplan-claude-kit-0.1.0.zip -DestinationPath C:\path\to\target-project -Force
```

bash:

```bash
unzip -o build/distributions/fetchplan-claude-kit-0.1.0.zip -d /path/to/target-project
```

The target project must already provide the fetchplan addon task:

```bash
gradlew.bat fetchplanAnalyzeMethod
```

See `docs/fetchplan-claude/README.md` in the unpacked kit for usage guidance.
