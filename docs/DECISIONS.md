(Additive report-layer decision)

## Decision — Declared fetch plan paths are not treated as ground truth in report interpretation

Context:
- fetch plans are manually authored;
- manual fetch plans may contain overfetch;
- unsupported analysis zones may prevent static confirmation of a declared path.

Decision:
- keep current raw comparison concepts (`Covered`, `Missing`, `Extra`, `Uncertain`) for backward compatibility;
- interpret leaf declared paths not matched by analysis as **declared but not statically confirmed** rather than automatic analyzer defect;
- add a derived report-layer breakdown for declared-not-confirmed paths:
  - declared under uncertainty;
  - possible analyzer gap;
  - probable overfetch;
  - structural/container declared paths.
- ignore standard Jmix/system leaf fields in fetch-plan comparison (`id`, version/audit/delete timestamps/users)
  so they do not inflate covered/missing/extra interpretation.

Rationale:
- improves report readability without changing extraction semantics;
- avoids overstating analyzer certainty;
- better matches the real workflow where fetch plans may be broader than true runtime need.
# Decisions

This file records important design decisions for future development.

---

## D-001 RawTree stores unique semantic path facts

RawTree does not store every usage event.
It stores unique semantic path facts.

Reason:
- keeps raw model compact
- matches fetch-plan comparison use case
- easier normalization

Consequence:
- repeated identical accesses collapse into one raw path
- if usage counting is needed later, it should be added as a separate layer

---

## D-002 Raw and canonical models are separate

RawTree and PathTree are intentionally different.

Reason:
- raw model must preserve semantic technical transitions
- canonical model must remain simple and user-facing

Consequence:
- `ALIAS`, `COLLECTION_ELEMENT`, `UNKNOWN_BREAK` live in raw model
- canonical paths hide technical nodes

---

## D-003 Analyzer is best-effort, not a proof engine

The analyzer is explicitly best-effort.

Reason:
- Java/Spring/Jmix contain dynamic patterns that are not practical to cover fully in v1
- practical value comes from reducing blind spots, not from claiming total certainty

Consequence:
- unsupported patterns must surface as uncertainty
- reports must not overstate certainty

---

## D-004 Interproc is layered on top of existing engine

Interprocedural analysis is implemented as a continuation-based extension over the existing engine.

Reason:
- reuse existing payload and continuation model
- avoid rewriting engine core
- keep interproc incremental

Consequence:
- simple method-body continuation works in v1
- more advanced interproc modes can evolve later

---

## D-005 Recursion guard is conservative

Visited-state guard is intentionally conservative.

Reason:
- prevent infinite recursion early
- stability is more important than perfect revisit semantics in v1

Consequence:
- some legitimate revisits may be pruned
- acceptable for current pilot stage

---

## D-006 Structural fetch plan paths should not dominate comparison output

Container prefixes such as `employee`, `type`, `subcontracts` should not create noisy `Extra` results.

Reason:
- fetch plans are hierarchical
- user cares mostly about terminal/relevant leaf coverage

Consequence:
- comparison should be leaf-aware / structural-aware

---

## D-007 Structural parent paths are containers, not analyzed terminal coverage

When a deeper useful descendant path is analyzed, structural parent/container paths should not be emitted as analyzed
terminal paths only because they acted as intermediate anchors.

Reason:
- user-facing analyzed output should focus on useful terminal/leaf paths;
- structural parents such as `agreement`, `agreement.sides`, or `vendorInfo` create noise if emitted as standalone
  analyzed terminals while deeper descendants already exist;
- hierarchical/structural coverage, if needed later, should be a separate reporting concept rather than mixed into
  `Analyzed` or `Covered`.

Consequence:
- structural parent paths with deeper analyzed descendants should stay suppressed from terminal analyzed output;
- `Covered` should not be inflated by those parent/container paths;
- non-leaf declared container paths should not surface as missing when deeper analyzed descendants exist;
- leaf/path-level strictness remains at the declared leaf level.

---

## D-008 Getter-like method calls become canonical paths only for metadata-backed entity properties

Getter naming alone is not sufficient evidence that a method represents a real fetch-plan property.

Reason:
- entity classes may expose computed/business getters such as conversion helpers;
- paths like `codeAsEnum` or `nameAsEnum` are false fetch-plan properties when no metadata-backed field/property exists;
- adding such paths creates report noise in `Missing`, `Extra`, and derived breakdown sections.

Consequence:
- `getX()` / `isX()` on entity-like classes should emit canonical segment `x` only when `x` is backed by a real entity field/property;
- computed/business getters without backing metadata must be ignored as fetch-plan path segments;
- when a getter-like entity method is not metadata-backed, analyzer should prefer best-effort body inspection to recover
  real backing persistent reads rather than emitting a fake computed path;
- body inspection must remain narrow, recursion-guarded, and limited to safe same-entity source-resolved cases.

---

## D-009 Computed getter body extraction is allowed only as a conservative same-entity backing-read recovery step

Computed/business getters may be useful not as fetch-plan properties themselves, but as wrappers around real entity reads.

Reason:
- domain entities often expose convenience methods like `getCodeAsEnum()` or `getInstanceName()`;
- dropping those calls entirely can hide real persistent reads used inside them;
- emitting the computed getter name itself is incorrect for fetch-plan comparison.

Consequence:
- analyzer may inspect simple zero-arg computed getter bodies declared on the same entity class;
- extracted output must consist only of real metadata-backed properties found inside the body;
- recursion/depth guards are required so chained computed getters do not loop forever;
- service calls, repository reloads, and broader side-effect analysis remain out of scope.