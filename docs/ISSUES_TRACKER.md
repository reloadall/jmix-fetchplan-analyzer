# Issues Tracker

This file tracks discovered bugs, vulnerabilities, weak spots, unsupported edge cases, and reliability concerns.

Rules:

- Do not delete old items.
- If an item is fixed, keep it in this file and mark it as resolved.
- Prefer concise factual notes.
- Add evidence, affected area, and current status when known.

Status values used below:

- `OPEN`
- `RESOLVED`
- `PARTIALLY MITIGATED`

---

## ISSUE-001 — Very low behavioral regression coverage in core analyzer

- Status: `PARTIALLY MITIGATED`
- Area: tests / whole analyzer core
- Found during: repository exploration and initial test hardening
- Summary:
  The project initially had only a Spring context smoke test and almost no regression protection for analyzer behavior.
- Evidence:
  At the start of test work, the visible test suite effectively contained only `FetchPlanAnalyzerJmixTest.contextLoads()`.
- Mitigation so far:
  Added focused tests for:
  - `PathComparator`
  - `RawTreeNormalizer`
  - `RawTreeUncertaintyExtractor`
  - `NameExpressionHandler`
- Remaining gap:
  Engine orchestration, statement handlers, fetch plan extraction, and interprocedural analysis are still weakly covered.

---

## ISSUE-002 — Engine orchestration layer is still weakly protected by tests

- Status: `PARTIALLY MITIGATED`
- Area: `AstPathEngine`, statement payload flow, continuations
- Found during: initial regression planning
- Summary:
  The queue/continuation orchestration layer is central to correctness but currently lacks targeted regression tests.
- Risk:
  Small changes in statement flow or continuation behavior may silently break path extraction.
- Candidate coverage:
  - simple getter-chain flow through engine;
  - `if / else` branching;
  - `foreach` continuation behavior;
  - return statement handling;
  - top-level method call transitions.
- Mitigation so far:
  Added a first engine-level regression test for a minimal getter-chain scenario through:
  - `AstPathEngine`
  - `StatementsPayloadHandler`
  - `ExpressionStatementHandler`
  - `NameExpressionHandler`
  - `MethodCallExpressionHandler`
  - `RawTreeNormalizer`
  - `PathTreeFlattener`
- Added a branch-level regression test for `if / else` path extraction through engine orchestration.
- Added a loop-level regression test for `foreach` collection-element traversal through engine orchestration.
- Remaining gap:
  Branching, loops, return flow, and interprocedural transitions are still not covered at engine level.
  Local alias rebinding is now exercised by scenario integration, but still does not have a narrowly isolated core engine regression test.
  Alias chain is also now exercised by scenario integration, but still does not have a narrowly isolated core engine regression test.

---

## ISSUE-003 — Interprocedural analysis is high-risk and largely untested

- Status: `PARTIALLY MITIGATED`
- Area: `InterprocMethodResolver`, `InterprocCallPlanner`, `InterprocReturnResolver`, `InterprocArgumentBinder`
- Found during: repository exploration
- Summary:
  Interprocedural analysis depends on several conservative heuristics and currently has almost no regression coverage.
- Risk:
  Future changes may alter same-class calls, bean-based target resolution, argument rebinding, or return rebinding without immediate detection.
- Candidate coverage:
  - same-class happy-path call;
  - `this.someMethod(...)`;
  - simple return-based rebinding;
  - recursion guard scenario;
  - simple unambiguous Spring bean implementation resolution.
- Mitigation so far:
  Added scenario-driven integration coverage in `DocumentScenarioIntegrationTest` for:
  - baseline document flow with cross-service calls;
  - explicit `this.someMethod(...)` flow;
  - simple value-call assignment flow;
  - same-class helper return rebinding.
  Added focused regression coverage in `ReturnRebindingCanonicalOutputTest` for helper-body entity reads preserved across
  same-class value-calls whose helper return value is non-entity / not rebindable, including foreach element binding,
  constructor-argument usage boundary, and nested method-call argument usage (`currentSum.add(line.getCost())`).
- Remaining gap:
  Interproc behavior is still not deeply isolated by focused core regression tests for recursion guard, ambiguous resolution, or broader bean-resolution cases.

---

## ISSUE-004 — Expression handler ordering is behavior-sensitive

- Status: `OPEN`
- Area: `ExpressionResolver` and ordered `ExpressionHandler` implementations
- Found during: code review of expression layer
- Summary:
  `ExpressionResolver` iterates handlers in order and commonly returns after the first meaningful result.
- Risk:
  Adding or reordering handlers can change semantics of existing supported patterns.
- Evidence:
  The resolver stops early once it gets a non-empty or uncertain result, making handler order part of observable behavior.
- Candidate coverage:
  - name resolution before method-based handlers;
  - collection `get()` before generic method-call field extraction;
  - pass-through / map / interproc interactions.

---

## ISSUE-005 — Fetch plan extraction flow is not yet protected by focused tests

- Status: `PARTIALLY MITIGATED`
- Area: `FetchPlanExtractor`, `FetchPlanPathSet`
- Found during: test-gap analysis
- Summary:
  The distinction between `allPaths` and `leafPaths` is important for `Covered` and `Extra`, but there are no focused regression tests for it yet.
- Risk:
  A subtle change in fetch plan traversal could increase false `Extra` or incorrect `Covered` results.
- Candidate coverage:
  - nested fetch plans;
  - leaf-only detection;
  - empty nested plans;
  - multiple sibling branches.
- Mitigation so far:
  Added focused tests for:
  - null fetch plan;
  - nested fetch plan extraction;
  - empty nested fetch plan treated as leaf;
  - multiple sibling branches.
- Remaining gap:
  End-to-end interaction between fetch plan extraction and full report generation is still not directly covered.

---

## ISSUE-006 — Visited-state / recursion guard remains a correctness hotspot

- Status: `OPEN`
- Area: `VisitedKeyFactory`, `AnalysisStep`, interproc recursion control
- Found during: repository exploration and architecture review
- Summary:
  The analyzer depends on conservative visited-state logic to prevent infinite recursion while avoiding incorrect pruning.
- Risk:
  If the visited key is too coarse, legitimate paths may be skipped; if too fine, recursion may reappear.
- Evidence:
  This risk is called out explicitly in repository guidance and architecture notes.
- Candidate coverage:
  - controlled recursive same-class call;
  - same method reached with different bindings or anchors.

---

## ISSUE-007 — Repeated source-root scanning and Java file parsing may significantly slow real analysis runs

- Status: `RESOLVED`
- Area: `SourceRootsResolver`, `SourceMethodResolver`, `InterprocMethodResolver`
- Found during: review of source-loading flow for live MBean usage
- Summary:
  Source roots are re-scanned and Java files are re-parsed repeatedly across analysis runs and interprocedural transitions.
- Evidence:
  - `AstPathAnalyzeMBean.analyzeRawTree(...)` calls `sourceRootsResolver.resolveMainJavaSourceRoots()` on each analysis run.
  - `SourceMethodResolver.resolve(...)` re-runs file lookup and `StaticJavaParser.parse(javaFile)`.
  - `InterprocMethodResolver.resolve(...)` again calls `resolveMainJavaSourceRoots()`, file lookup, and `StaticJavaParser.parse(javaFile)`.
- Why this matters:
  In real usage on large service methods and interproc-heavy flows, the same source files may be parsed many times during one application lifetime.
- Current recommendation:
  Introduce application-lifetime caches under the assumption that source files do not change until app restart.
- Suggested implementation order:
  1. cache resolved source roots;
  2. cache `className -> javaFile Path`;
  3. cache `Path -> CompilationUnit`;
  4. only later consider method-level caches if still needed.
- Resolution:
  Added shared `SourceAnalysisCache` used by `SourceMethodResolver` and `InterprocMethodResolver`.
  The cache stores application-lifetime source roots, `fqcn -> Path` lookups including negative results,
  and `Path -> CompilationUnit` parsing results.

---

## ISSUE-008 — Scenario integration fixture still uses path-set fetch plan holder instead of production-like Jmix FetchPlan

- Status: `OPEN`
- Area: scenario integration tests / fetch plan fixture realism
- Found during: implementation of `fetchplan-jmix-test-scenarios`
- Summary:
  The first scenario module uses a plain path-set fixture for declared fetch plan paths instead of a real Jmix `FetchPlan` object.
- Why this matters:
  It validates analyzer path extraction and comparison integration, but does not yet verify the full production-like fetch-plan construction path.
- Current rationale:
  Avoid broad analyzer/test infrastructure changes at this step while still introducing a living-documentation scenario module.
- Suggested next step:
  Add a production-like Jmix fetch plan fixture for the scenario once it can be done without broad refactoring.

---

## ISSUE-009 — Scenario currently exposes structural parent `shippingAddress` in canonical output for return-based rebinding flow

- Status: `RESOLVED`
- Area: normalization / return-based rebinding / scenario integration
- Found during: `fetchplan-jmix-test-scenarios` integration test setup
- Summary:
  In the document scenario, same-class helper return rebinding currently produces both `shippingAddress` and `shippingAddress.city` in analyzed canonical paths.
- Why this matters:
  The user-facing canonical model usually aims to minimize structural noise, but current behavior exposes the parent path in this scenario.
- Resolution:
  Updated return-flow handling so that a top-level `return` in an interprocedural callee with
  `continuationOnFinish` does not mark the returned anchor raw node as `TERMINAL` when that node is
  only being passed back to the caller for continued field access.
  Added focused regression coverage in `ReturnRebindingCanonicalOutputTest` for:
  - `sameClassHelperReturnRebindingShouldKeepOnlyLeafCanonicalPath()`
  - `explicitThisCallReturnRebindingShouldKeepOnlyLeafCanonicalPath()`
  - `valueCallReturnRebindingShouldKeepOnlyLeafCanonicalPath()`
  and kept the cast continuation control case green.
  Updated scenario expected canonical paths so return-based rebinding now produces only
  `shippingAddress.city` instead of `shippingAddress` plus `shippingAddress.city`.
- Additional note:
  The cast-based continuation scenario currently does **not** emit the structural parent; it produces only
  `shippingAddress.city`, so the parent-path behavior remains scenario-specific rather than universal.
- Follow-up resolution:
  Terminal marking for getter/member path resolution was later tightened so getter-created raw nodes are
  created as `INTERMEDIATE` by default and become `TERMINAL` only in the actual usage context
  (expression statement / condition / return / other boundary handling). This prevents structural parent
  leakage such as `type` alongside `type.code` for plain getter chains.

---

## ISSUE-010 — Scenario fetch-plan fixture is still path-set based, so full production-like fetch-plan construction path is not yet regression-covered end-to-end

- Status: `OPEN`
- Area: scenario integration / fetch plan realism
- Found during: setup and documentation of `fetchplan-jmix-test-scenarios`
- Summary:
  Scenario integration currently compares analyzer output against declared path fixtures using `FetchPlanPathSet`, not a real Jmix `FetchPlan` instance.
- Why this matters:
  The scenario validates realistic source traversal and comparison semantics, but still skips one part of the production-like fetch-plan creation chain.
- Current action:
  Keep the scenario small and stable; document this explicitly rather than broadening analyzer/test infrastructure in the same step.
- Suggested next step:
  Introduce a production-like scenario fetch-plan fixture only when it can be done without broad refactoring or context startup.

---

## ISSUE-010A — Interprocedural return rebinding previously treated helper side effects as fallback return values

- Status: `RESOLVED`
- Area: `InterprocReturnResolver` / interprocedural return rebinding
- Found during: Step 4 production hardening for return rebinding semantics
- Summary:
  `InterprocReturnResolver` previously fell back to helper-body `sideEffects` when `returnValues` were empty, which could incorrectly bind a caller variable to an internal field read performed inside the helper.
- Why this matters:
  A helper like `document.getType().getCode(); return null;` may legitimately record `type.code` as an internal usage, but that usage is not the returned object and must not become the caller-side binding for a value-call continuation.
- Resolution:
  Tightened return rebinding so value-call continuation uses only actual resolved return values. Helper-body `sideEffects` remain available as recorded usages inside the callee walk, but they are no longer treated as fallback returned objects/values. Added focused regression coverage for:
  - negative case: side-effect-only helper plus `return null` must not produce fake rebinding such as `type.code.city`;
  - positive case: actual returned `document.getShippingAddress()` still rebinds and yields `shippingAddress.city`.

---

## ISSUE-010B — Value-call planning contract around eager return rebinding plus callee-body traversal was implicit and needed regression protection

- Status: `RESOLVED`
- Area: `InterprocCallPlanner.planValueCall(...)` / interprocedural value-call semantics
- Found during: Step 5 audit of eager return rebinding vs target-method continuation
- Summary:
  `planValueCall(...)` eagerly resolves the return value for caller-variable rebinding and still schedules callee-body traversal. That combination is intentional, but without explicit contract/tests it risked future regressions such as duplicate canonical output, leaked structural parents, or fake continuation from helper side effects.
- Required contract:
  1. eager return rebinding exists so caller continuation can continue from the returned entity anchor;
  2. callee traversal remains necessary so helper-body side effects/usages can still be preserved;
  3. callee return traversal must not turn the returned anchor into standalone terminal noise when caller continues deeper.
- Resolution:
  Kept the behavior conservative and unchanged architecturally, but documented the contract directly in `InterprocCallPlanner.planValueCall(...)` and added focused regression coverage for:
  - pure return + caller continuation -> only `shippingAddress.city`;
  - side effect + real return -> `type.code` and `shippingAddress.city`;
  - side effect + unresolved return -> no caller rebinding from side effects.

---

## ISSUE-010C — `ExpressionResolver` comments implied merge-friendly semantics while implementation was first-meaningful-result

- Status: `RESOLVED`
- Area: `ExpressionResolver` / handler-order contract
- Found during: Step 6 contract-hardening audit
- Summary:
  `ExpressionResolver` behavior is intentionally order-sensitive, but in-code comments implied that compatible handler results might be merged. That was misleading because implementation returned as soon as the first non-empty or uncertain result appeared.
- Why this matters:
  Handler order is observable analyzer behavior. Misstating the contract could lead to unsafe future changes that assume later handlers may refine or merge the same expression result.
- Resolution:
  Updated `ExpressionResolver` comments to explicitly document the current first-meaningful-result strategy and added focused regression coverage proving that:
  - handler order is deterministic;
  - the first non-empty result wins;
  - an uncertain first meaningful result also stops later handlers.

---

## ISSUE-006A — Visited-key binding fingerprint previously ignored dispatch targets and terminal-only semantics

- Status: `RESOLVED`
- Area: `VisitedKeyFactory` / recursion guard / branch identity
- Found during: Step 7 visited-key audit
- Summary:
  The visited key already distinguished method, payload/AST position, current raw anchor, binding node ids, and uncertainty, but binding fingerprint did not include dispatch target sets or the `terminalOnly` flag.
- Why this matters:
  Two semantically different analysis branches could otherwise collide when they shared the same binding name and raw-node ids but differed in dispatch/fan-out semantics or terminal-only boundary behavior.
- Resolution:
  Minimally extended the bindings fingerprint in `VisitedKeyFactory` to include:
  - sorted dispatch target class names;
  - `terminalOnly` flag.
  Added focused regression coverage proving that visited keys now differ for:
  - different raw anchors;
  - different binding target nodes;
  - different dispatch target sets;
  - different `terminalOnly` binding state.

---

## ISSUE-011 — Scenario uncertainty coverage currently depends on integration wiring gaps rather than a dedicated isolated core regression test

- Status: `PARTIALLY MITIGATED`
- Area: uncertainty reporting / scenario integration / expression support wiring
- Found during: adding `inspectDocumentWithUnknownBreak(Document document)`
- Summary:
  The uncertainty scenario originally depended on missing expression-handler wiring in `DocumentScenarioIntegrationTest`.
- Evidence:
  `DocumentScenarioIntegrationTest` now wires the production-like expression handlers, including:
  - `ConditionalExpressionHandler`
  - `EnclosedExpressionHandler`
  - `CastExpressionHandler`
  After this fix, the old conditional-based uncertainty scenario stopped being an uncertainty-case and had to be replaced.
- Why this matters:
  Scenario integration should validate real analyzer behavior rather than artifacts of incomplete test wiring.
- Current action:
  Fixed the test wiring to be closer to production/manual analyzer assembly.
  Replaced the old conditional uncertainty scenario with a static helper call pattern that still produces uncertainty under production-like wiring.
- Suggested next step:
  Add a focused core regression test for the genuinely unsupported static-helper uncertainty pattern, or document and narrow the exact unsupported semantics more explicitly.

---

## ISSUE-012 — Stream scenario coverage currently reflects only minimal chained `map(MethodRef)` support, not broader stream semantics

- Status: `PARTIALLY MITIGATED`
- Area: stream expression support / scenario integration
- Found during: adding `inspectDocumentWithStreamMap(Document document)`
- Summary:
  Scenario coverage now exercises a narrow supported stream case using chained method references, but this must not be overstated as general stream support.
- Evidence:
  `MapMethodCallExpressionHandler` supports only `map(...)` with exactly one `MethodReferenceExpr`, while `PassThroughMethodCallExpressionHandler` only forwards a small set of stream-like methods.
- Current covered case:
  `document.getLines().stream().map(DocumentLine::getProduct).map(Product::getSku)` -> `lines.product.sku`.
- Remaining limitation:
  Lambda-based terminal operations such as `forEach(product -> product.getSku())` and broader stream semantics are still not guaranteed by this coverage.

---

## ISSUE-013 — Object-reference terminal usage policy is undefined

- Status: `RESOLVED`
- Area: canonical semantics / terminal usage policy
- Found during: ISSUE-009 return rebinding cleanup
- Summary:
  It is not yet defined whether obtaining, binding, or returning an entity reference without deeper property access should produce a canonical parent path, uncertainty, or no analyzed path.
- Example:
  `Address address = resolveShippingAddress(document);`
  without later `address.getCity()`.
- Why this matters:
  The analyzer must distinguish between:
  - a structural/intermediate anchor used only to continue analysis;
  - a real terminal usage of an entity reference;
  - a case that should be reported as uncertain.
- Resolution:
  Defined and implemented the policy as follows:
  1. if an association/reference getter is accessed and no deeper entity field is emitted from it, emit the association as a valid leaf path;
  2. if the same association is used only as a structural anchor for deeper access, suppress the structural parent and emit only deeper leaf paths;
  3. explicit standalone getter usage remains terminal and may coexist with deeper leaf usage;
  4. the same behavior now applies to simple interprocedural return rebinding and null-check style association access.
- Implementation note:
  This was implemented in canonical normalization/flattening semantics, not by blindly marking assignment initializers as `TERMINAL`.
- Regression coverage added:
  - direct assignment-only association access;
  - direct assignment plus deeper access;
  - explicit standalone plus deeper access;
  - interproc return without deeper caller access;
  - interproc return with deeper caller access;
  - null-check association access;
  - raw normalization guard that prevents uncertain/deeper-unknown descendants from being silently collapsed into a certain parent leaf.

---

## ISSUE-014 — Scenario-driven coverage is now substantially broader, but still concentrated in one document-oriented fixture graph

- Status: `PARTIALLY MITIGATED`
- Area: scenario integration / regression strategy
- Found during: end-of-session scenario coverage review
- Summary:
  The scenario module now covers many supported patterns, but most of them still live in one document-oriented fixture family.
- Covered in current scenario module:
  - baseline document flow;
  - `if / else`;
  - collection `get(0)`;
  - explicit `this.someMethod(...)`;
  - value-call assignment;
  - local alias rebinding;
  - alias chain;
  - cast-based continuation;
  - narrow chained `stream().map(MethodRef)`;
  - one uncertainty / `UNKNOWN_BREAK` case.
- Why this matters:
  This is strong progress for regression coverage, but future changes may still benefit from additional independent scenario families so that one fixture graph does not dominate all realistic-growth work.
- Current action:
  Keep the document-oriented scenario as the main living-documentation fixture and record this concentration explicitly.
- Suggested next step:
  When a new real unsupported case appears, prefer adding it either as a clearly isolated root method or as a second scenario family only if separation improves stability or realism.

---

## ISSUE-015 — Full `:fetchplan-jmix:test` suite currently fails in Spring context smoke test due to missing Jmix UI bean

- Status: `RESOLVED`
- Area: test context wiring / `FetchPlanAnalyzerJmixTest`
- Found during: post-BSOD recovery verification
- Summary:
  The focused analyzer regression tests pass, but the full `:fetchplan-jmix:test` task currently fails in `FetchPlanAnalyzerJmixTest.contextLoads()`.
- Evidence:
  The failing context load reports:
  - `UnsatisfiedDependencyException` for `ui_RelativeIntervalConverter`
  - nested `NoSuchBeanDefinitionException` for `io.jmix.ui.app.propertyfilter.dateinterval.RelativeDateTimeMomentProvider`
- Why this matters:
  The milestone-specific scenario and interproc regressions are green, but the full module test task is not currently clean, so commit readiness must be reported with that limitation.
- Root cause:
  `FetchPlanAnalyzerJmixTestConfiguration` imports `FetchPlanAnalyzerJmixConfiguration`, which depends on `UiConfiguration.class`.
  That pulls Jmix UI beans into the smoke-test context, including `ui_RelativeIntervalConverter`, which requires
  `RelativeDateTimeMomentProvider`. The test configuration did not provide that bean.
- Resolution:
  Added a test-scope bean in `FetchPlanAnalyzerJmixTestConfiguration` that supplies a Mockito mock of
  `RelativeDateTimeMomentProvider`.
  This keeps the smoke test broad enough to verify analyzer module context loading while avoiding unrelated failure
  from missing auxiliary UI test wiring.
- Verification:
  After the fix, the following commands pass:
  - `gradlew.bat :fetchplan-jmix:test --tests io.github.reloadall.fetchplan.analyzer.jmix.FetchPlanAnalyzerJmixTest`
  - `gradlew.bat :fetchplan-jmix:test`
  - `gradlew.bat :fetchplan-jmix:test --tests io.github.reloadall.fetchplan.analyzer.jmix.scenario.DocumentScenarioIntegrationTest`
  - `gradlew.bat :fetchplan-jmix:test --tests io.github.reloadall.fetchplan.analyzer.jmix.interproc.ReturnRebindingCanonicalOutputTest`

---

## ISSUE-016 — General collection-injected Spring worker dispatch remains only partially supported

- Status: `PARTIALLY MITIGATED`
- Area: interprocedural Spring bean resolution / foreach dispatch / scenario integration
- Found during: investigation of `List<DocumentWorker>` worker-fan-out pattern from real Jmix/Spring projects
- Summary:
  The analyzer now supports a narrow foreach-based worker fan-out pattern, but broader collection-injected
  Spring worker dispatch semantics are still not implemented.
- Desired scenario:
  `DocumentScenarioService.inspectDocumentWithWorkers(Document document)` iterates over constructor-injected
  `List<DocumentWorker>` and calls `worker.process(document)` for two `@Service` implementations:
  `ContractWorker` and `CustomerWorker`.
- Desired canonical paths:
  - `contract.number`
  - `contract.customer.name`
- Current behavior gap:
  The original gap was that resolution was built around one resolved target type/method and did not support
  collection element type extraction plus multi-implementation dispatch for Spring bean collections.
- Mitigation so far:
  Added lower-level support for:
  - supported collection container detection for `List<T>`, `Collection<T>`, and `Iterable<T>`;
  - generic element type extraction for simple supported shapes;
  - read-only multi-bean implementation lookup in `SpringBeanImplementationResolver` with deterministic ordering.
- Implemented scope:
  Implemented a narrow foreach fan-out shape:
  `for (Worker worker : workers) { worker.process(document); }`
  where `workers` is `List<T>` / `Collection<T>` / `Iterable<T>`, element implementations are resolved,
  the loop variable directly scopes the method call, and the scenario is now covered by focused regression
  and scenario integration tests.
- Evidence:
  - `DocumentScenarioIntegrationTest.analyzesWorkerCollectionScenarioAndMatchesFixturePaths()` is now enabled and green.
  - `AstPathEngineForEachWorkerFanOutTest.fansOutForeachOverInjectedWorkersToAllResolvedImplementations()` covers
    the minimal worker fan-out path through engine/interproc wiring.
- Remaining unsupported patterns:
  - `workers.forEach(...)`;
  - stream/lambda-based worker dispatch;
  - `supports(...)` filtering or other selective worker execution semantics;
  - qualifier / `@Primary` / ordering-aware worker selection;
  - registry-style worker dispatch such as `Map<String, Worker>`;
  - broader generalized collection-injected bean dispatch semantics.
- Suggested next step:
  Keep support narrow and incremental. Remaining candidates:
  1. `workers.forEach(...)` / stream/lambda worker dispatch;
  2. qualifier / ordering / filtering semantics;
  3. broader collection and registry shapes such as `Map<String, Worker>`.

---

## ISSUE-017 — Lombok-style constructor-injected single-bean service-call scenario relies on declared source field type, not Lombok-generated constructor analysis

- Status: `PARTIALLY MITIGATED`
- Area: scenario integration / interprocedural field-based target resolution
- Found during: addition of a focused Lombok-style single-bean scenario
- Summary:
  The analyzer can resolve a service call through a Lombok-style `private final` field when the declared field type is
  visible in source, but this does not imply any deeper analysis of generated constructor bytecode or broader Lombok semantics.
- Covered scenario:
  `SyntheticLombokScenarioService.inspectDocumentWithLombokServiceCall(RootDocument document)` calls
  `routeInfoFinder.findRouteInfo(document, log)` through a `@RequiredArgsConstructor`-style final field and continues to
  `detail.parentDetail.document.routeInfo.code`.
- Why this matters:
  This matches a common Spring/Lombok source shape while keeping support honest and narrow.
- Remaining limitation:
  The analyzer still relies on declared source field types and existing interproc rules; it does not inspect generated
  constructors, Lombok transformations, repository reloads, multi-origin returns, or recursive helper flows.

---

## ISSUE-018 — Chained cross-service return rebinding is covered for a narrow synthetic happy path, but broader multi-service return semantics remain risky

- Status: `PARTIALLY MITIGATED`
- Area: interprocedural return rebinding / scenario integration
- Found during: addition of chained synthetic finder scenario
- Summary:
  The analyzer now covers a narrow chain where one service returns an entity and later services continue from that
  returned origin, but this should not be overstated as general support for arbitrary chained service graphs.
- Covered scenario:
  `SyntheticLombokScenarioService.inspectDocumentWithChainedFinders(RootDocument document)` now verifies:
  - `detail.parentDetail.document.routeInfo.vendorInfo.code`
  - `detail.parentDetail.document.routeInfo.groupInfo.code`
- Remaining limitation:
  This does not yet cover multi-origin returns, recursive helpers, repository reload flows, or broader ambiguous
  rebinding semantics across more complex service graphs.

---

## ISSUE-019 — Multi-origin return rebinding remains uncovered until dedicated synthetic scenario is verified

- Status: `OPEN`
- Area: interprocedural return rebinding / multi-origin return semantics
- Found during: planning of synthetic multi-origin agreement scenario
- Summary:
  The analyzer still needs explicit verification for a method that may return one of two different origin paths and then
  allows the caller to continue nested access from that returned entity.
- Desired scenario:
  `AgreementFinder.findAgreement(VendorInfo vendorInfo, GroupInfo groupInfo, ScenarioLog log)` returning from either
  `vendorInfo.getAgreement()` or `groupInfo.getAgreement()`, followed by caller access to
  `agreement.getSides().getCounterparty().getName()`.
- Desired canonical paths:
  - `detail.parentDetail.document.routeInfo.vendorInfo.agreement.sides.counterparty.name`
  - `detail.parentDetail.document.routeInfo.groupInfo.agreement.sides.counterparty.name`
- Risk:
  Current analyzer behavior may follow only one return origin, drop a branch, emit structural parents, or surface
  unnecessary uncertainty.

---

## ISSUE-020 — Covered semantics for declared parent paths became ambiguous after structural parent cleanup in return rebinding flows

- Status: `OPEN`
- Area: comparison / reporting semantics / parent-path coverage policy
- Found during: validation of real anonymized reports after return rebinding cleanup
- Summary:
  After cleanup of structural parent noise in return-rebinding output, aggregate report metrics worsened
  (`Analyzed: 45 -> 41`, `Covered: 16 -> 12`) even though no leaf paths were lost and `Missing` did not increase
  (`28 -> 28`).
- Evidence:
  Removed analyzed/covered paths were only structural parents:
  - `iii`
  - `yyy.cccc.dddd.eeee.iiii`
  - `yyy.cccc.dddd.eeee.iiii.aaa`
  - `yyy.cccc.dddd.eeee.nnnn`
  and each still had deeper analyzed descendants after the change.
- Why this matters:
  This indicates a policy question in comparison/report semantics rather than an extraction regression.
  Current reporting may undercount `Covered` when a declared parent path is no longer emitted explicitly,
  even though deeper analyzed descendants still prove traversal through that parent.
- Important note:
  `ReturnStatementHandler` cleanup should **not** be reverted on the basis of these metrics alone.
  The observed drop is currently consistent with structural parent cleanup, not with loss of useful leaf paths.
- Current interpretation:
  - no increase in `Missing`;
  - no evidence of lost leaf paths;
  - likely parent-path coverage policy / reporting issue.
- Recommended next step:
  Add focused comparator/report-level tests before any behavior change, specifically for the policy:
  whether a declared parent path should be considered `Covered` when one or more deeper analyzed descendants remain.
 - Status update:
   Focused comparator tests were added to lock the policy that structural parent/container paths are not standalone
   analyzed terminals, do not inflate `Covered`, and should stay suppressed from `Missing` when they are non-leaf
   declared container paths with deeper analyzed descendants.

---

## ISSUE-021 — Helper-based type-check calls are not general narrowing primitives; first supported shape stays helper-guard plus explicit cast

- Status: `PARTIALLY MITIGATED`
- Area: cast continuation / branch-dependent subtype access / scenario integration
- Found during: planning of the next dynamic type-check scenario
- Summary:
  Real code often uses helper-based boolean type checks before subtype-specific access, but current support should remain
  narrow: the supported signal is the explicit Java cast inside the guarded branch, not the helper call itself.
- Covered scenario:
  `SyntheticLombokScenarioService.inspectLineWithTypeGuardAndCast(BaseLine line)` verifies extraction of:
  - `header.agreement.sides.counterparty.name`
  from a branch guarded by `lineTypeGuard.isSpecificLine(line)` followed by `(SpecificLine) line`.
- Remaining limitation:
  Helper calls such as `isSpecificLine(...)` are still not treated as general subtype-narrowing primitives without an
  explicit cast or other supported continuation signal.
- Status update:
  Added a second focused scenario for negative helper guard + early return + explicit cast:
  `SyntheticLombokScenarioService.inspectLineWithNegativeTypeGuardAndCast(BaseLine line)`.
  The supported signal remains the explicit cast after the guard rather than the helper method itself.

---

## ISSUE-022 — Boolean helper body path extraction in conditions required explicit interproc return-walk support for control-flow and non-return boolean expressions

- Status: `RESOLVED`
- Area: interprocedural helper-body extraction / condition analysis / return resolver
- Found during: addition of synthetic boolean-helper-body scenario
- Summary:
  A boolean helper used in an `if` condition was entered interprocedurally, but entity paths read inside the helper body
  were not preserved unless they directly participated in a simple returned path node.
- Evidence:
  New synthetic scenario
  `SyntheticLombokScenarioService.inspectLineWithBooleanHelperBody(BaseLine line)`
  initially expected `parent.metaName` but produced `[]`.
  Trace showed that analysis entered:
  - `LineTypeGuard.isNotInAllowedKind(BaseLine, List<Class<?>>)`
  - nested `LineTypeGuard.isKind(Class<?>, HasSyntheticMeta)`
  - `document.getMetaName()`
  but returned no analyzed terminal path because:
  - condition expressions inside `if` were not walked by `InterprocReturnResolver`;
  - `foreach` bodies inside the helper were not traversed there;
  - boolean return expressions such as `return actualKind != null;` did not mark helper-body reads as terminal usage.
- Resolution:
  Added the smallest targeted support in `InterprocReturnResolver` for:
  - evaluating `if` conditions during interproc return walking;
  - traversing `foreach` iterable/body during interproc return walking;
  - evaluating binary return expressions and marking participating resolved nodes as terminal.
  Also updated `IfStatementHandler` to resolve condition expressions in the main engine flow.
- Verified result:
  The boolean-helper-body synthetic scenario now produces canonical path:
  - `parent.metaName`
- Important non-expansion note:
  This does **not** make helper methods subtype-narrowing primitives.
  It only preserves entity-path reads performed inside boolean helper bodies used in conditions.

---

## ISSUE-023 — Nested value-call argument interproc continuation was not preserved through inline argument binding

- Status: `RESOLVED`
- Area: interprocedural argument binding / condition terminal policy / synthetic scenario coverage
- Found during: addition of nested synthetic value-call argument scenario
- Summary:
  An inline nested interprocedural call used as an argument to another interprocedural call could be resolved, but
  downstream leaf access was not consistently preserved in final canonical output.
- Evidence:
  New synthetic scenario
  `SyntheticLombokScenarioService.inspectDocumentWithNestedValueCallArgument(RootDocument document)`
  initially expected:
  - `detail.parentDetail.document.routeInfo.code`
  but first produced `[]`, and after an intermediate condition-marking change produced noisy parent/container paths such as
  `detail` and `detail.parentDetail.document.routeInfo` in unrelated scenario outputs.
- Root cause:
  Two interacting issues were exposed:
  1. nested interproc argument resolution depended on condition-expression handling to preserve terminal leaf usage through
     scalar/null guards around the returned inline value;
  2. a too-broad condition-marking fallback started marking non-scalar entity anchor names in binary conditions, which
     reintroduced structural parent noise in existing scenario outputs.
- Resolution:
  Added focused synthetic coverage for the inline nested value-call argument pattern and tightened `IfStatementHandler`
  so binary-condition fallback marks only scalar-like name operands (e.g. `String`, primitive/wrapper, `CharSequence`) as
  terminal when direct expression resolution is empty.
  This preserves leaf reads like `routeInfo.getCode()` behind `if (code != null)` without reintroducing structural
  container paths for entity references such as `routeInfo != null` or `agreement != null`.
- Verified result:
  The nested synthetic scenario now produces canonical path:
  - `detail.parentDetail.document.routeInfo.code`
  and previously green scenario/document regression tests remain green.

---

## ISSUE-024 — Raw report semantics can overstate declared-only paths as analyzer failures because manual fetch plans may contain overfetch

- Status: `PARTIALLY MITIGATED`
- Area: comparison/report interpretation
- Found during: review of real anonymized report with high declared-only surface and large uncertainty
- Summary:
  A plain `Covered / Missing / Extra / Uncertain` view is not expressive enough when declared fetch plans are manually
  authored and may intentionally or accidentally contain overfetch.
- Why this matters:
  Users may misread declared-but-not-confirmed leaf paths as definite analyzer bugs, even when they are:
  - under uncertainty;
  - likely overfetch;
  - or only weakly suggestive of a possible analyzer gap.
- Mitigation so far:
  Added a derived report-layer breakdown for declared-not-confirmed paths:
  - `Declared under uncertainty`
  - `Possible analyzer gap`
  - `Probable overfetch`
  - `Structural/container paths`
- Important note:
  This is a readability/interpretation improvement only.
  Core extraction and comparison semantics remain unchanged.

---

## ISSUE-025 — Default/system fields such as `id` can create misleading comparison noise in declared-only and analyzed-only path sets

- Status: `RESOLVED`
- Area: comparison/report interpretation
- Found during: review of real fetch-plan reports where `.id` leaf paths appeared as declared-not-confirmed noise
- Summary:
  Jmix default/system fields like `id` should not be treated as meaningful fetch-plan coverage requirements, but they can
  otherwise appear in `Missing`, `Extra`, or derived declared-not-confirmed groups.
- Resolution:
  Added comparison-layer filtering for standard system leaf fields:
  - `id`
  - `version`
  - `createTs`
  - `createdBy`
  - `updateTs`
  - `updatedBy`
  - `deleteTs`
  - `deletedBy`
  Filtering applies to leaf system-field paths only and does not remove parent/container entity paths.

---

## ISSUE-026 — Getter-like business methods were treated as canonical fetch-plan properties by naming convention alone

- Status: `RESOLVED`
- Area: getter path extraction / canonical path correctness / report noise
- Found during: investigation of remaining declared-not-confirmed paths after system/default field filtering
- Summary:
  The analyzer used pure getter naming convention (`getX()` / `isX()`) to emit canonical path segment `x`, even when the
  method was a computed/business getter and `x` was not a real metadata-backed entity property.
- Example:
  `entity.getCodeAsEnum()` incorrectly produced canonical path `codeAsEnum` although no such persistent/Jmix property existed.
- Root cause:
  `MethodCallExpressionHandler` decapitalized getter-like method names directly and created `DIRECT` raw nodes without
  validating that the target entity type actually contained a backed property with that name.
- Resolution:
  Added metadata-backed property validation in getter extraction:
  - resolve the scope expression type when possible;
  - if the scope type is an entity-like class, emit getter-derived property only when a non-static, non-transient,
    non-`@Transient` backing field with that name exists;
  - otherwise keep previous conservative fallback behavior for unresolved/non-entity scope types.
- Verified behavior:
  - persistent getter still works: `entity.getCode()` -> `code`;
  - fake computed getter path is suppressed: `entity.getCodeAsEnum()` does not emit `codeAsEnum`;
  - simple computed getter body analysis now recovers backing reads, e.g. `entity.getCodeAsEnum()` -> `code`;
  - nested normal getter chains such as `order.getType().getCode()` remain supported.
- Remaining limitation:
  Computed getter body extraction is still intentionally narrow and best-effort. Complex logic, external calls,
  repository reloads, arbitrary helper graphs, and broader side-effect semantics remain unsupported.

---

## ISSUE-027 — Computed getter body extraction can regress silently because it relies on narrow source-resolved helper semantics

- Status: `PARTIALLY MITIGATED`
- Area: getter body analysis / source-resolved entity helper semantics
- Found during: implementation of computed getter body extraction after fake computed path suppression
- Summary:
  The analyzer now recovers backing entity property reads from simple computed getters, but this support relies on a
  deliberately narrow body walker over source-resolved zero-arg same-entity methods.
- Covered shapes:
  - `return getCode();`
  - `return getCode() + " - " + getName();`
  - `return SomeEnum.fromCode(getCode());`
  - local variable rebinding such as `String code = getCode(); return code;`
  - direct field access such as `return code + " - " + name;`
  - recursion-guarded chaining across same-entity zero-arg computed getters.
- Risk:
  Future changes may accidentally reintroduce fake computed paths, lose recovered backing reads, or break recursion guards.
- Mitigation so far:
  Added focused regression coverage in `AstPathEngineComputedGetterTest` for:
  - metadata-backed getter;
  - computed getter wrapper around persistent getter;
  - instance-name style concatenation;
  - local-variable rebinding;
  - direct field access;
  - recursion guard.
- Remaining limitation:
  Complex branching, service/repository calls, localization helpers, and arbitrary helper graphs remain outside this support scope.

---

## ISSUE-028 — Unqualified inherited protected converter-helper call was not resolved through abstract superclass method body

- Status: `RESOLVED`
- Area: interprocedural method resolution / inherited protected methods / synthetic converter hierarchy
- Found during: addition of synthetic converter-hierarchy regression scenario
- Summary:
  An unqualified inherited protected call such as `createParams(document)` from a concrete subclass method did not enter
  the abstract superclass method body, so analyzer output was empty and declared leaf paths fell under uncertainty.
- Covered scenario:
  `SyntheticDocumentConverter.createDto(RootDocument document)` extends `SyntheticBaseConverter<RootDocument>` and calls
  inherited protected method `createParams(document)`, where `createParams(HasSyntheticDocument document)` is declared in
  the abstract superclass and reads `agreement.sides.counterparty.name`.
- Root cause:
  `InterprocMethodResolver` previously searched only the concrete owning type for unqualified calls and did not walk the
  superclass chain. After superclass resolution was added, scenario output still stayed empty because
  `InterprocReturnResolver` preserved expression-statement path reads but did not merge them into the accumulated return-walk
  result for helper-style methods returning DTO objects.
- Resolution:
  Added narrow superclass-chain lookup in `InterprocMethodResolver` for unqualified/owning-type calls, including same-file
  abstract superclass declarations. Also updated `InterprocReturnResolver` so preserved expression-statement reads are merged
  into collected interproc results, allowing DTO-producing helper bodies to contribute entity path usage even when the return
  value itself is not a meaningful entity node.
- Verified result:
  The synthetic converter-hierarchy scenario now resolves and produces canonical path:
  - `agreement.sides.counterparty.name`
- Remaining limitation:
  This is still not a broad Java inheritance model. Support remains intentionally narrow and best-effort:
  no overload-heavy inheritance dispatch, no arbitrary abstract/proxy behavior, no reflection, and no generalized generic
  substitution beyond what is needed for the covered scenario.

---

## ISSUE-029 — Pre-reload path reads inside private helper can collapse into deeper technical/system leaf instead of preserving the pre-boundary anchor path

- Status: `RESOLVED`
- Area: interprocedural return/body side-effect preservation / private helper path extraction / unsupported reload boundary
- Found during: synthetic base-parameter + private-helper converter scenario
- Summary:
  In a converter-style private helper flow, the analyzer successfully resolves inherited base method, binds
  `SyntheticCalculation -> BaseCalculation`, enters private helper `getTarget(calculation)`, and reaches the pre-reload
  path `parent.lineBase`, but final canonical output can collapse to a deeper technical/system leaf such as
  `parent.lineBase.id` instead of preserving the pre-boundary anchor `parent.lineBase`.
- Evidence:
  Synthetic scenario `SyntheticCalculationConverter.createDto(SyntheticCalculation line)` initially produced:
  - actual: `parent.lineBase.id`
  - expected: `parent.lineBase`
  Trace showed:
  - target method `createParams(BaseCalculation)` resolved;
  - private helper `getTarget(BaseCalculation)` resolved;
  - local bindings for `parent` and `lineBase` created;
  - unsupported-reload-like helper path ultimately preserved `lineBase.getId()` as the observable terminal leaf.
- Exact first stop point:
  Not method resolution and not argument binding.
  The first semantic divergence appears in return/body preservation around the unsupported-reload-like helper branch:
  the analyzer keeps the deeper helper-body leaf (`id`) rather than the intended pre-reload anchor (`lineBase`).
- Likely affected components:
  - `InterprocReturnResolver`
  - potentially path-terminal policy around helper-body side effects / unresolved return boundaries
- Important note:
  This is different from previously covered inherited-helper and generic-parameter scenarios.
  The unresolved area is specifically the policy for preserving pre-boundary anchor paths before an unsupported reload-like step.
- Resolution:
  Added a narrow pre-boundary anchor preservation rule in `InterprocReturnResolver` for technical id access used only as
  unsupported reload boundary preparation. When an expression statement resolves only a technical `.id` leaf from a real
  entity anchor, analyzer now preserves the parent entity anchor node as the useful terminal path instead of exposing only
  the technical leaf.
- Verified result:
  The synthetic converter scenario now produces canonical path:
  - `parent.lineBase`
  instead of:
  - `parent.lineBase.id`
- Important limitation note:
  This is **not** repository reload support and does not implement `dataManager.load(...)`, `Id.of(...)`, or fetch-plan
  builder semantics. It only preserves the last confirmed pre-boundary entity anchor before an unsupported reload/id
  boundary.

---

## ISSUE-030 — Boundary/query return incorrectly inherits origins from filter arguments

- Status: `RESOLVED`
- Area: interprocedural return-origin analysis / boundary-query calls / value-call rebinding
- Found during: focused regression for `fullChainBoundaryReturnMustNotInheritFilterArgumentOrigins(FullChainAct act)`
- Summary:
  A helper method that returns a boundary/query-like method call with root-derived filter arguments can incorrectly make
  those filter arguments look like origins of the returned query result. When caller code later iterates or otherwise reads
  the returned result, the analyzer may attach result-entity reads under filter paths such as `dateStart`, `contract`, or
  `currency`.
- Evidence:
  The focused failing regression currently expects only:
  - `dateStart`
  - `dateFinish`
  - `contract`
  - `currency`

  but the analyzer produces:
  - `dateStart.docLine`
  - `dateStart.cost`
  - `dateStart`
  - `dateFinish.docLine`
  - `dateFinish.cost`
  - `dateFinish`
  - `contract.docLine`
  - `contract.cost`
  - `contract`
  - `currency.docLine`
  - `currency.cost`
  - `currency`
- Required interprocedural origin contract:
  1. Method arguments may produce usage paths.
     Example: `act.getDateStart()` -> `dateStart`.
  2. Method arguments may be expandable origins inside the callee body only for usage of that exact parameter.
     Example: `groupTotals(liabilityLines)` may emit `liabilityLines.rate` / `liabilityLines.cost` from
     `for (Line line : lines)`.
  3. Method arguments must not automatically become origins of the method return value.
  4. Caller result variable rebinding is allowed only from explicit resolved return expressions rooted in caller-origin paths.
     Example: `return document.getShippingAddress()` may rebind result to `shippingAddress`.
  5. A return expression that is an unsupported/boundary/query call with filter arguments must not use those filter
     arguments as return origins.
     Example: `return transactionQuery(dateStart, dateFinish, contractId, currencyId)` must not rebind result to
     `dateStart` / `dateFinish` / `contract` / `currency`.
  6. If return origin is unresolved/external/query result, the caller result variable must remain unbound/external.
  7. Later reads from that unbound/external result must not attach under filter argument paths.
  8. Parameter usage analysis and return-origin analysis are separate concerns.
- Audit note / likely violation point:
  The likely violation is in `InterprocReturnResolver.handleReturnStatement(...)`. When direct return-expression resolution
  through `context.getExpressionResolver().resolveAll(...)` is empty, it falls back to
  `collectBoundaryArgumentUsages(...)` and returns those argument-usage nodes as the return value. That fallback is useful
  for preserving source-side usage paths at boundary calls, but it is unsafe when the caller treats the returned
  `ExpressionResolutionResult` as a rebindable return origin.

  The problematic path is:
  - `InterprocCallPlanner.planValueCall(...)` eagerly calls `interprocReturnResolver.resolveReturnValue(...)`;
  - `InterprocReturnResolver.handleReturnStatement(...)` sees `return transactionQuery(dateStart, dateFinish, ...)`;
  - `InterprocMethodCallExpressionHandler.resolveAll(...)` / direct expression resolution cannot resolve a meaningful
    query-result origin;
  - `handleReturnStatement(...)` falls back to `collectBoundaryArgumentUsages(...)`;
  - `planValueCall(...)` receives those filter-argument usage nodes as `returnResult` and binds the caller variable
    `paymentTransactions` to them;
  - subsequent `generatePayments(paymentTransactions, act)` expands `Transaction` reads under the filter argument paths.
- Resolution:
  Separated return-origin analysis from boundary-argument usage preservation in
  `InterprocReturnResolver.handleReturnStatement(...)`. Directly resolved return expressions still produce return origins,
  so valid rebinding such as `return document.getShippingAddress()` remains supported. When a return expression cannot be
  resolved as a real origin and only boundary/query arguments can be collected, those argument paths are now preserved as
  side effects, not returned as `returnValues`. This prevents `InterprocCallPlanner.planValueCall(...)` from binding caller
  result variables such as `paymentTransactions` to filter arguments like `dateStart`, `contract`, or `currency`.
- Follow-up decision:
  A broader parameter-scoped task/model refactor is not needed for this concrete failing scenario. The immediate bug was
  fixed at the return-origin boundary, and explicit parameter return remains supported by regression coverage such as
  `returnedParameterShouldRebindExplicitOrigin`, where `return address` correctly rebinds to caller-origin path
  `shippingAddress.city`. Keep parameter-scoped interproc task refactoring as a future option only if a concrete bug shows
  that shared `targetBindings` causes incorrect parameter usage.
- `terminalOnly` audit after fix:
  The `ValueBinding.terminalOnly` marker is still useful as a boundary/usage-only marker after the ISSUE-030 fix. It lets
  argument binding preserve source-side usages for scalar or otherwise non-expandable parameters while preventing normal
  name resolution, entry-anchor selection, and caller-variable rebinding from treating those bindings as expandable entity
  origins. Current meaningful dependencies include:
  - scalar/filter arguments such as `dateStart` / `dateFinish` staying observable as terminal usages;
  - forwarded repository/boundary argument scenarios preserving pre-boundary source paths;
  - non-rebindable helper-body reads such as `groupTotals(liabilityLines)` still expanding only the actual collection
    parameter that is safe to traverse;
  - visited-key fingerprinting distinguishing terminal-only usage bindings from expandable bindings.

  No production cleanup is currently justified solely by the ISSUE-030 fix. Some scalar-like condition handling may be a
  separate future cleanup candidate, but it should only be changed with a concrete regression target because it protects
  existing condition/body-read scenarios from structural parent noise.