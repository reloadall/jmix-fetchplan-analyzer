# fetchplan-jmix-test-scenarios

This module is a **source fixture / living documentation** module for the analyzer.

## Purpose

- provide small but realistic Spring/Jmix-like service code for analyzer regression tests;
- document which analyzer cases already work on a branchy, domain-shaped scenario;
- keep the fixture code in regular `src/main/java`, because the analyzer scans normal Java source roots.

## Important notes

- This module is **not** a production addon/application.
- It intentionally looks Spring/Jmix-like, but it does **not** aim to start a real Spring Boot or Jmix application context.
- It does **not** contain repositories, data stores, Liquibase changelogs, datasource setup, or persistence runtime logic.
- Services are regular Spring-style beans via `@Service` and constructor injection.
- Entities are Jmix/JPA-like source fixtures with minimal annotations needed to look realistic and compile.

## Usage in tests

- analyzer tests from `fetchplan-jmix` use this module as test input / source fixture;
- new supported analyzer cases can be added here as living service/entity code;
- expected canonical paths and declared fetch-plan fixture data live next to the scenario.

## Currently covered root scenarios

The document scenario currently uses separate root methods with separate expected path sets.

- `inspectDocument(Document document)`
  - baseline happy-path document flow;
  - covers direct getter chains, cross-service calls, `foreach`, and same-class return-based rebinding.

- `inspectDocumentBranch(Document document)`
  - focused `if / else` branch flow.

- `inspectFirstLine(Document document)`
  - focused collection access flow using `document.getLines().get(0)`.

- `inspectDocumentWithThisCall(Document document)`
  - focused explicit `this.someMethod(...)` flow.

- `inspectDocumentWithValueCall(Document document)`
  - focused value-call initializer / assignment flow.

- `inspectDocumentWithLocalAlias(Document document)`
  - focused local alias rebinding flow:
    `DocumentType type = document.getType(); type.getCode();`

- `inspectDocumentWithAliasChain(Document document)`
  - focused alias-chain flow:
    `DocumentType first = document.getType(); DocumentType second = first; second.getName();`

- `inspectDocumentWithCast(Document document)`
  - focused cast-based continuation flow:
    `Object address = document.getShippingAddress(); Address castedAddress = (Address) address; castedAddress.getCity();`
  - current analyzed canonical output is `shippingAddress.city` without the structural parent.

- `inspectDocumentWithStreamMap(Document document)`
  - focused minimal stream/map scenario;
  - uses chained method references:
    `document.getLines().stream().map(DocumentLine::getProduct).map(Product::getSku);`
  - current intended supported output is `lines.product.sku`.

- `inspectDocumentWithUnknownBreak(Document document)`
  - focused uncertainty scenario;
  - uses a static helper call pattern so the analyzer reports uncertainty instead of a certain path.

These scenarios are intentionally split into separate root methods so that expected canonical paths stay focused and readable.

Not yet documented as scenario-covered here:

- broader stream / lambda semantics beyond minimal chained method-reference mapping.

## Workflow

This module is the preferred scenario-driven workflow for analyzer development.

1. A developer changes realistic Spring/Jmix-like code here.
2. That scenario code is treated as the desired analyzer target behavior.
3. Expected canonical paths and fetch-plan fixture paths are updated to the desired analysis result.
4. The scenario integration test is run.
5. If the test fails, the scenario should not be simplified just to make it pass.
6. The analyzer should be improved with the smallest safe change, or the limitation should be documented honestly.
7. Once green, the scenario becomes a new supported regression case and living documentation example.

Two working modes are useful:

- **Baseline mode** — temporarily records the current actual analyzer behavior, but only with explicit documentation of known limitations.
- **Development mode** — scenario code and expected paths define desired analyzer semantics and should not be weakened to fit a wrong output.

## How to add a new analyzer scenario

1. Add realistic code to an existing root flow or introduce a separate root method if the new pattern should be isolated from the current baseline.
2. Add or update expected canonical paths.
3. Add or update declared fetch-plan fixture paths.
4. Run:

   ```bash
   gradlew.bat :fetchplan-jmix-test-scenarios:compileJava
   gradlew.bat :fetchplan-jmix:test --tests io.github.reloadall.fetchplan.analyzer.jmix.scenario.DocumentScenarioIntegrationTest
   ```

5. If the test fails:
   - do not simplify or remove the scenario code;
   - do not weaken expected paths unless explicitly documenting a baseline limitation;
   - analyze which analyzer capability is missing;
   - implement the smallest safe analyzer change or document unsupported/uncertain behavior.
6. After the fix, rerun the scenario test and relevant regression tests.

## Suggested workflow for analyzer changes

When a scenario test fails:

1. Treat scenario code as the desired analyzer target behavior.
2. Do not simplify or remove scenario code just to make the test pass.
3. Do not weaken expected paths unless explicitly documenting a baseline limitation.
4. Analyze which analyzer capability is missing.
5. Implement the smallest safe analyzer change.
6. Keep existing behavior stable.
7. Run the scenario integration test and relevant regression tests.

The final report should include:
- what scenario change exposed;
- why analyzer failed;
- what analyzer code changed;
- what tests were run;
- remaining limitations.

## Current limitation

- declared fetch plan is still represented as a path-set fixture, not a production-like Jmix `FetchPlan` instance.
- the module intentionally does not start a Spring or Jmix application context.