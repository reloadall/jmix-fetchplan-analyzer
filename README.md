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

## Current capabilities

The current pilot supports:

- linear getter chains
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
- **Extra** — declared leaf fetch plan paths that are not used
- **Uncertain** — paths affected by unsupported or partially supported patterns

## High-level pipeline

1. Parse Java source and locate target method
2. Build `RawTree`
3. Normalize `RawTree` into canonical `PathTree`
4. Flatten canonical paths
5. Extract declared paths from Jmix fetch plan
6. Compare analyzed vs declared paths
7. Produce report

## Repository structure

- `docs/ARCHITECTURE.md` — architecture overview
- `docs/SUPPORTED_SCOPE.md` — supported/unsupported patterns
- `docs/ROADMAP.md` — v1 / v1.1 / v2
- `docs/DECISIONS.md` — important design decisions
- `docs/EXAMPLES.md` — sample inputs and outputs
- `docs/FEEDBACK_TEMPLATE.md` — how to report unsupported cases

## Intended usage

Current intended usage is **manual/internal analysis**:
- run analysis for a real service method
- inspect normalized paths
- compare with fetch plan
- review `Missing`, `Extra`, and `Uncertain`
- collect unsupported patterns for future iterations

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