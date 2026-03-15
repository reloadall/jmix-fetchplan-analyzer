# Supported Scope

## Supported in v1 pilot

### Intra-method patterns

- direct getter chains
- `if / else`
- `foreach`
- `list.get(0)`
- alias rebinding through local variables
- cast-based continuation
- `stream().map(MethodRef)` with basic pass-through methods
- top-level method calls
- `UNKNOWN_BREAK`

### Interprocedural patterns

- same-class method calls
- `this.someMethod(...)`
- simple interface-based bean call when implementation can be resolved unambiguously
- value-call in initializer/assignment
- return-based rebinding in simple cases
- recursion guard

### Output layers

- raw tree
- normalized canonical paths
- comparison with Jmix fetch plan
- uncertainty section

## Partially supported / best effort

- stream pipelines beyond the supported subset
- multiple returns with non-identical result nodes
- simple bean resolution in Spring context
- uncertain cases based on `UNKNOWN_BREAK`

## Not supported yet

- reflection-based access
- dynamic proxies with ambiguous runtime targets
- collection-injected Spring bean fan-out, e.g. `List<Worker>`
- advanced stream pipelines
- complex overload resolution
- qualifiers / primary resolution logic in full generality
- factory-based bean resolution
- arbitrary expression semantics
- full event/usage counting
- full path certainty scoring

## Important interpretation rule

The tool should be treated as a **best-effort static analyzer**:
- `Missing` is a strong signal
- `Extra` is useful but still context-dependent
- `Uncertain` means the tool encountered unsupported or partial analysis zones

## Recommended usage model

Use it as:
- an internal review assistant
- a fetch plan relevance detector
- a source of feedback and unsupported cases

Do not treat it as:
- a proof engine for arbitrary Java
- a strict CI gate without project-specific validation