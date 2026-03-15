# Architecture

## Overview

The analyzer is structured as a layered pipeline:

1. source resolution
2. AST traversal
3. raw semantic model
4. canonical path normalization
5. fetch plan extraction
6. comparison and reporting

## Main layers

### 1. Source layer

Responsible for locating and parsing the target Java source method.

Main responsibilities:
- locate `src/main/java`
- resolve target class and method
- obtain `MethodDeclaration`

Typical class:
- `SourceMethodResolver`

---

### 2. Engine layer

Responsible for AST traversal and path extraction.

Main responsibilities:
- orchestrate analysis steps
- dispatch statement handlers
- dispatch expression handlers
- maintain analysis context
- create continuations
- perform interprocedural transitions
- guard against recursion

Key concepts:
- `AnalysisStep`
- `StepPayload`
- `StatementsPayload`
- `Continuation`
- `StatementHandler`
- `ExpressionHandler`

Important design principle:
the engine is orchestration-oriented and delegates semantic work to handlers and resolvers.

---

### 3. Raw semantic model

Responsible for storing extracted semantic path facts.

Main responsibilities:
- record path segments
- record collection element transitions
- record alias transitions
- record breaks and uncertain points

Key types:
- `RawTree`
- `RawNode`
- `FlowKind`
- `UsageKind`

Important note:
`RawTree` stores **unique semantic path facts**, not every usage event.

---

### 4. Normalization layer

Responsible for converting raw semantic facts into canonical entity paths.

Main responsibilities:
- drop technical nodes such as `ALIAS`
- treat `COLLECTION_ELEMENT` as transparent
- stop at `UNKNOWN_BREAK`
- build canonical `PathTree`

Key types:
- `RawTreeNormalizer`
- `PathTree`
- `PathNode`
- `PathTreeFlattener`

---

### 5. Fetch plan layer

Responsible for converting Jmix fetch plans into canonical path sets.

Main responsibilities:
- resolve fetch plan by name
- extract declared paths
- distinguish between all paths and leaf paths

Key types:
- `FetchPlanResolver`
- `FetchPlanExtractor`
- `FetchPlanPathSet`

---

### 6. Comparison / reporting layer

Responsible for comparing analyzed paths with declared fetch plan paths.

Main responsibilities:
- compute Covered / Missing / Extra / Uncertain
- hide structural noise where possible
- produce human-readable report

Key types:
- `PathComparator`
- `PathComparisonResult`
- `AnalysisReport`
- `AnalysisReportFormatter`

## Analysis flow

### Step 1
Resolve target method from Java source.

### Step 2
Run AST engine and build `RawTree`.

### Step 3
Extract uncertainty zones from `RawTree`.

### Step 4
Normalize `RawTree` into `PathTree`.

### Step 5
Flatten `PathTree` into canonical path strings.

### Step 6
Resolve Jmix fetch plan and flatten it into canonical declared paths.

### Step 7
Compare analyzed paths against declared fetch plan paths.

### Step 8
Render final report.

## Intra-method analysis

Current intra-method support includes:
- simple getter chains
- branching
- alias rebinding
- collection element access
- simple stream projections
- top-level method calls

## Interprocedural analysis

Current interproc support is intentionally limited.

Supported:
- same-class method calls
- interface-based bean call when implementation is resolved unambiguously
- simple parameter rebinding
- value-call in initializer/assignment
- recursion guard

Not fully supported:
- complex bean fan-out
- overloaded ambiguity resolution
- reflection-based dispatch
- full return-flow semantics for arbitrary methods

## Why raw model and canonical model are separated

The raw model records semantic traversal facts, including technical transitions:
- alias
- collection element
- unknown break

The canonical model is the end-user representation:
- flattened entity paths
- no technical nodes
- cleaner comparison against fetch plans

This separation keeps the engine expressive while keeping the final result simple.