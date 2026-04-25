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

## Scenario coverage in `fetchplan-jmix-test-scenarios`

The scenario module now contains focused root methods with separate expected path sets.

Currently covered by scenario integration:

- baseline document flow via `DocumentScenarioService.inspectDocument(Document document)`;
- `if / else` branch flow via `inspectDocumentBranch(Document document)`;
- collection element access via `inspectFirstLine(Document document)` using `get(0)`;
- explicit same-class `this.someMethod(...)` flow via `inspectDocumentWithThisCall(Document document)`;
- value-call initializer / assignment flow via `inspectDocumentWithValueCall(Document document)`;
- local alias rebinding via `inspectDocumentWithLocalAlias(Document document)`.
- alias chain via `inspectDocumentWithAliasChain(Document document)`.
- cast-based continuation via `inspectDocumentWithCast(Document document)`.
- minimal `stream().map(MethodRef)` chain via `inspectDocumentWithStreamMap(Document document)`.
- uncertainty / `UNKNOWN_BREAK` behavior via `inspectDocumentWithUnknownBreak(Document document)`.

Important note:

- these cases are scenario-covered as separate root methods, not merged into one ambiguous expected-path set;
- broader stream semantics beyond the minimal chained method-reference pattern are **not** yet documented as scenario-covered in this module.

Do not treat it as:
- a proof engine for arbitrary Java
- a strict CI gate without project-specific validation