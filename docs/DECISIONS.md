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