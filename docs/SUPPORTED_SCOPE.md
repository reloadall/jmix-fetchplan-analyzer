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
- narrow foreach fan-out for collection-injected Spring worker beans via `List<T>` / `Collection<T>` / `Iterable<T>` when loop variable directly scopes a method call and all implementations are resolved
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
- general collection-injected Spring bean dispatch beyond the narrow foreach pattern;
- `workers.forEach(...)` and stream/lambda-based worker dispatch;
- `supports(...)`-style filtering / selective worker execution;
- qualifier / `@Primary` / ordering-sensitive worker selection semantics;
- registry-style worker dispatch such as `Map<String, Worker>`;
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
- narrow collection-injected worker fan-out via `inspectDocumentWithWorkers(Document document)` for:
  `for (Worker worker : workers) { worker.process(document); }`
  where `workers` is `List<T>` / `Collection<T>` / `Iterable<T>` and Spring bean implementations are resolved.
- Lombok-style constructor-injected single-bean interprocedural call via
  `SyntheticLombokScenarioService.inspectDocumentWithLombokServiceCall(RootDocument document)`
  where a `private final` service field declared in source is used to call another service method and
  caller code continues from the returned entity with a leaf getter.
- chained cross-service return rebinding via
  `SyntheticLombokScenarioService.inspectDocumentWithChainedFinders(RootDocument document)`
  where one finder returns an entity rooted at the source document and later finders continue from that
  returned entity to deeper leaf fields.
- helper-guarded explicit cast continuation via
  `SyntheticLombokScenarioService.inspectLineWithTypeGuardAndCast(BaseLine line)`
  where a boolean helper guard does not act as subtype proof by itself, but a guarded explicit Java cast allows
  subtype-specific path extraction.
- negative helper guard + early return + explicit cast continuation via
  `SyntheticLombokScenarioService.inspectLineWithNegativeTypeGuardAndCast(BaseLine line)`
  where a negative boolean helper guard causes early return, and subtype-specific extraction still relies on the
  explicit cast that follows the guard.

Still not scenario-covered as supported generalized worker dispatch:

- broader collection-injected worker dispatch beyond the narrow foreach pattern, including `workers.forEach(...)`, stream/lambda dispatch, filtering, qualifier-sensitive selection, and map-based worker registries.

Important note:

- these cases are scenario-covered as separate root methods, not merged into one ambiguous expected-path set;
- broader stream semantics beyond the minimal chained method-reference pattern are **not** yet documented as scenario-covered in this module.
- collection-injected worker fan-out is currently supported only for the narrow foreach pattern exercised by `inspectDocumentWithWorkers(Document document)`.
- structural parent/container paths with deeper analyzed descendants are treated as containers, not as standalone analyzed
  terminal paths; they should not inflate `Covered`, and hierarchical coverage is not a separate report concept yet.

Do not treat it as:
- a proof engine for arbitrary Java
- a strict CI gate without project-specific validation