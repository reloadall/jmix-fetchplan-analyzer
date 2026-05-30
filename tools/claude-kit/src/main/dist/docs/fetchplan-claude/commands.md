# FetchPlan Claude Commands

Copy and adapt these commands in a target project that already provides `fetchplanAnalyzeMethod`.

Use fully qualified class names for `fetchplan.rootType` when known. Prefer output files, especially for JSON, because console output may contain Gradle or logging warnings.

## Plain JSON report

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.json
```

## Markdown report

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=markdown \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.md
```

## Comparison with inline expected paths

Useful for short experiments only. Prefer `fetchplan.expectedPathsFile` for longer lists.

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.expectedPaths=customer.name,lines.product.name \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total-comparison.json
```

## Comparison with expected paths file

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.expectedPathsFile=build/reports/fetchplan/order-calculate-total.expected-paths.txt \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total-comparison.json
```

## Output to file

Always prefer the addon `fetchplan.output` property:

```bash
gradlew.bat fetchplanAnalyzeMethod \
  -Pfetchplan.className=com.example.app.service.OrderService \
  -Pfetchplan.methodName=calculateTotal \
  -Pfetchplan.rootParam=order \
  -Pfetchplan.rootType=com.example.app.entity.Order \
  -Pfetchplan.format=json \
  -Pfetchplan.output=build/reports/fetchplan/order-calculate-total.json
```

Avoid relying on stdout for machine-readable JSON because Gradle or logging output can make the stream invalid JSON.
