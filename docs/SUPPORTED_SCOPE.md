# Supported Scope

## Supported in v1 pilot

### Intra-method patterns

- direct getter chains
- getter-like method calls are converted to canonical properties only when they match a metadata-backed entity field/property
- simple computed getter bodies on entity classes are inspected best-effort to recover real backing property reads
  such as `getCodeAsEnum() -> code` or `getInstanceName() -> code, name`
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
- narrow inherited protected helper call on superclass chain for unqualified calls from the concrete subclass method
- simple interface-based bean call when implementation can be resolved unambiguously
- narrow foreach fan-out for collection-injected Spring worker beans via `List<T>` / `Collection<T>` / `Iterable<T>` when loop variable directly scopes a method call and all implementations are resolved
- value-call in initializer/assignment
- return-based rebinding in simple cases
- helper-body entity reads for same-class/interprocedural value-calls even when the helper return value is non-entity or otherwise not rebindable;
  in such cases the analyzer preserves caller-to-parameter binding and still traverses helper internals best-effort instead of dropping
  the whole call as an empty path tree
- recursion guard
- root-entity getter reads inside top-level method-call arguments, including narrow wrapper/pass-through argument shapes such as
  `IdLike.of(document.getContract())`, when the relevant fetch-plan usage is the pre-boundary reference anchor rather than
  repository/result internals

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
- computed/business entity getters are not emitted as fetch-plan properties when no metadata-backed property exists;
  e.g. `getCodeAsEnum()` must not create canonical path `codeAsEnum` just because it looks like a getter.

## Important interpretation rule

The tool should be treated as a **best-effort static analyzer**:
- `Missing` is a strong signal
- `Extra` is useful but still context-dependent
- `Uncertain` means the tool encountered unsupported or partial analysis zones

Report interpretation note:
- declared fetch plan paths are manual input, not mathematical ground truth;
- `Missing` / declared-not-confirmed paths must be interpreted as
  **"declared but not statically confirmed"**, not as automatic analyzer failure;
- when uncertainty overlaps a declared path, that path should be reviewed as uncertainty first,
  not as clear overfetch.
- default/system fields are ignored for fetch-plan comparison when they are declared as leaf paths;
- in particular, `id` is not treated as a required fetch-plan coverage path.
- computed/business getters are not fetch-plan paths unless backed by a real entity property.
- for simple zero-arg computed getters declared on the entity itself, analyzer now inspects the getter body best-effort
  and extracts real backing property reads instead of emitting fake computed property names.
- supported simple body shapes currently include direct property getters, string concatenation / binary composition,
  local variable rebinding before `return`, direct field access, and nested same-entity zero-arg computed getter calls.
- complex computed logic remains best-effort and limited; unsupported bodies should not reintroduce fake canonical paths.

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
- root getter reads inside method-call arguments via
  `DocumentScenarioService.inspectDocumentWithGetterArguments(Document document)`
  where canonical output keeps pre-boundary reads such as `dateStart`, `dateFinish`, `contract`, and `currency`
  without leaking `contract.id` / `currency.id` or paths from newly created return values.
  This supported shape is intentionally narrow: it applies to unsupported/external boundary calls that are not analyzed
  interprocedurally, and must not be read as a rule to mark every argument anchor of normal interprocedural calls as terminal.
- forwarded root-derived helper parameters into unsupported repository/external boundary via
  `DocumentScenarioService.inspectDocumentWithForwardedRepositoryArguments(Document document)`
  where a private helper receives root-derived scalar/reference values and then forwards them into a boundary call with no
  analyzable body; source-side root paths are preserved as final pre-boundary usages.
  Boundary/query return values are not considered root-derived return origins merely because their filter arguments are
  root-derived. For example, `return transactionQuery(dateStart, dateFinish, contractId, currencyId)` may preserve the
  filter argument usages, but the returned query result itself must remain unbound/external unless a separate supported
  return-origin expression proves otherwise.
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
- inherited protected converter-helper call via
  `SyntheticDocumentConverter.createDto(RootDocument document)`
  where a concrete subclass calls `createParams(document)` without explicit scope and the helper body is declared in
  abstract superclass `SyntheticBaseConverter<RootDocument>`.

Still not scenario-covered as supported generalized worker dispatch:

- broader collection-injected worker dispatch beyond the narrow foreach pattern, including `workers.forEach(...)`, stream/lambda dispatch, filtering, qualifier-sensitive selection, and map-based worker registries.

Important note:

- these cases are scenario-covered as separate root methods, not merged into one ambiguous expected-path set;
- broader stream semantics beyond the minimal chained method-reference pattern are **not** yet documented as scenario-covered in this module.
- collection-injected worker fan-out is currently supported only for the narrow foreach pattern exercised by `inspectDocumentWithWorkers(Document document)`.
- structural parent/container paths are not emitted merely because deeper descendants exist.
  They are emitted only when the parent property itself was actually accessed under the current policy.
  Real metadata-backed property access events such as `doc.getContract()` or `doc.getType()` therefore remain analyzed
  paths even when deeper property reads also exist.
- association/reference getter access now follows an explicit leaf policy:
  - a metadata-backed association accessed without deeper analyzed descendant usage is emitted as a valid leaf path;
  - if the same association is used only as an intermediate anchor for deeper access, only deeper leaf paths are emitted;
  - explicit standalone getter usage remains terminal and may coexist with deeper leaf usage;
  - the same rule applies to simple interprocedural return rebinding and null-check style association access.

Do not treat it as:
- a proof engine for arbitrary Java
- a strict CI gate without project-specific validation