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

- Status: `OPEN`
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
- Current action:
  `ReturnRebindingCanonicalOutputTest.returnedObjectUsedWithoutGetterNeedsExplicitPolicyBeforeAssertion()`
  remains disabled until this policy is explicitly decided.
- Candidate policy options:
  1. Emit the parent path, e.g. `shippingAddress`.
  2. Emit no path unless a deeper property is accessed.
  3. Report uncertainty.
  4. Make behavior context-dependent, but only if the distinction can be made reliably.
- Recommended next step:
  Collect 2–3 realistic examples from project code before deciding the policy.

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