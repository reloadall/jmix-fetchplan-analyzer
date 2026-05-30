# Expected Paths Files

Expected paths files are used with:

```bash
-Pfetchplan.expectedPathsFile=build/reports/fetchplan/example.expected-paths.txt
```

## Format

- Use UTF-8 text.
- Put one path per line.
- Blank lines are ignored.
- Lines starting with `#` are ignored.
- Prefer leaf paths.
- Uncertain paths should be commented or omitted.

Example:

```text
# Entity property paths rooted from the requested root parameter.
customer.name
lines.product.name

# Uncertain: dynamic service call; keep out of comparison unless explicitly testing it.
# approval.manager.email
```

## Path selection guidance

- Include only entity property paths rooted from the requested root parameter.
- Do not include DTO-only fields.
- Do not include scalar local variables.
- Do not include paths guessed only from names.
- Do not include a parent association when it is only a structural anchor for a deeper path.
- Include a parent association only when the association itself is directly used as a value.

Expected paths are review inputs, not authoritative truth. Keep manual review notes separately for dynamic or uncertain behavior.
