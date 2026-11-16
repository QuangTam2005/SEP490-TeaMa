# SEP490-TeaMa Workspace Agent Rules

## Mandatory Context-First Rule
Before starting any task, planning, code generation, or repository modification:
1. Read **all** Markdown specification files in `docs/*.md` (currently including `PRD.md`, `ARCHITECTURE.md`, `DESIGN.md`, `RULES.md`, `TASKS.md`, `PROGRESS.md`, plus any additional `.md` documentation files added later).
2. Strictly follow all architectural boundaries, database/API contracts, UI/UX standards, atomic commit rules, and red-line constraints defined in those documents.
3. After completing any task, mark `[x]` in `docs/TASKS.md` and update `docs/PROGRESS.md` before committing atomically (1 task/feature/fix = 1 commit).
