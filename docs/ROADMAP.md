# Roadmap

## v1 pilot

Goal:
usable internal tool for manual evaluation on real project code.

Includes:
- AST analysis baseline
- interproc v1
- raw tree
- canonical path normalization
- fetch plan comparison
- uncertainty reporting
- report formatting

Success criteria:
- works on selected real service methods
- detects meaningful missing paths
- does not produce excessive structural noise
- clearly signals uncertainty

## v1.1

Focus:
improve usefulness on real project cases.

Candidate items:
- improve uncertainty semantics
- better support for value calls
- better comparator/report presentation
- more real-world regression cases
- better diagnostics for unsupported patterns
- collection-injected Spring bean fan-out (`List<Worker>`) if confirmed as high-value

## v2

Focus:
coverage expansion and confidence.

Candidate items:
- richer Spring bean resolution
- more stream support
- smarter overload handling
- path/event diagnostics
- stronger uncertainty model
- better report integration
- CI-oriented mode after validation on real code

## Explicitly deferred

- full Java language coverage
- reflection-heavy semantics
- mathematically complete proof of fetch plan correctness
- universal static analysis for arbitrary enterprise code