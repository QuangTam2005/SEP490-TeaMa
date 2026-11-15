# PROJECT CODING & WORKFLOW CONVENTIONS
**Project:** SEP490 Capstone Copilot  

---

## 1. Git & Branching Strategy
- **Default Branch:** `main` (Production-ready / Stable releases).
- **Development Branch:** `develop` (Integration branch).
- **Feature Branches:** `feat/<module>-<task-id>` (Ví dụ: `feat/auth-TASK-01`).
- **Fix Branches:** `fix/<defect-id>` (Ví dụ: `fix/DEF-04`).
- **Commit Message Format:** Conventional Commits:
  - `feat(auth): [TASK-01] implement jwt login & registration`
  - `fix(kanban): [TASK-15] resolve drag-and-drop state mismatch`
  - `docs(prd): update prd v1.1 compliance notes`

---

## 2. Code Quality & Formatting
- **Linter & Formatter:** ESLint, Prettier.
- **Naming Conventions:** camelCase for variables/functions, PascalCase for classes/components, UPPER_CASE for constants/enums.
- **Type Safety:** Strict mode enabled for TypeScript.

---

## 3. Review & Merge Workflow
- Pull Requests must link to corresponding Kanban task ID in brackets `[TASK-XX]`.
- Webhook automatically moves task to `IN_REVIEW` upon PR opening, and `DONE` upon merge.
