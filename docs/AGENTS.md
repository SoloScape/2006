# Agent instructions

## Required editing workflow

- All repository edits must be made through the GitHub plugin. This applies to creating, updating, deleting, renaming, and moving files, including code, documentation, configuration, tests, assets, and this AGENTS.md file.
- Never edit files locally, including in the workspace, a clone, a worktree, or a temporary copy. Do not prepare local edits for a later commit or upload.
- Do not use local editors, apply_patch, shell commands, scripts, formatters, code generators, Git commands, the GitHub CLI, direct HTTP/API requests, or browser editing as a substitute for the GitHub plugin.
- Use the GitHub plugin to read the current target branch and file contents before making changes. Use the current file SHA for updates and deletions, preserve unrelated content, and verify the resulting remote files or diff through the plugin.
- Perform any required branch creation, commits, and pull request changes through the GitHub plugin as well.
- Local inspection is permitted only when it is read-only. Do not run tools that modify local files, including automatic fixes or generation of build/test artifacts. Use remote validation when available and report any checks that cannot be performed within this restriction.
- If the GitHub plugin is unavailable, lacks a required operation, or denies access, stop the affected edit and explain the limitation. Never fall back to local edits or another write mechanism.
- Apply these requirements to any delegated agents working on this repository.

## Pull request presentation standard

For **every pull request created or updated in this repository**, use the clean, structured GitHub Markdown presentation exemplified by [Barrows PR #56](https://github.com/SoloScape/SoloScape-2006/pull/56), unless the user requests a different style. Adapt the content to the PR's purpose; don't force audit-specific sections onto a small fix.

- **Title:** Use a descriptive, human-readable title naming the feature or area and the purpose. Avoid vague titles like "fix stuff".
- **Opening:** State the goal, scope, target branch and status (including **Draft** when applicable) in a brief introduction or blockquote.
- **At a glance:** Follow with a concise overview and, when comparing multiple issues, a compact Markdown summary table. Use priority markers such as 🔴 Critical, 🟠 High, 🟡 Medium, 🔵 Low or ⚪ Intentional when they carry meaning; do not use colour as the only signal.
- **Sections:** Group related work under clear headings. For multi-finding audits, use **numbered Markdown subheadings** (e.g. `### 1. Fix NPC special effects`), with priority groups where appropriate. For a feature/fix PR, organise by behaviour or component instead.
- **Evidence and action:** Explain **what happens now**, **what should happen**, **why it matters**, and **what was changed or remains to do**. Link directly to relevant repository files and lines. Use readable Markdown tables for comparisons, numbers and before/after behaviour.
- **Verification:** Clearly separate completed changes, automated tests, manual/in-game checks and outstanding verification. Never claim historical accuracy, successful tests or an implemented fix without evidence.
- **Historical claims:** Prefer primary period-specific sources. Label modern guides, approximations and unverified 2006 behaviour as such; do not present modern OSRS values as automatically authentic.
- **Finish:** Include remaining risks or intentional deviations, a short recommended order of work when useful, and the readiness/merge status.
- **Format:** Use native GitHub Markdown, short paragraphs, inline code, clean links and appropriate whitespace. Avoid pasted HTML, raw copied chat citations, redundant headings, giant unformatted text blocks and decorative clutter.
- **Checkbox convention:** Do **not** put checkboxes before numbered findings or section headings by default. Use `- [ ]` / `- [x]` only for an explicitly requested tracker or genuine action checklist; check items only after the work is verified. If the user says no checkboxes, respect that.
- **Preservation:** When restyling an existing PR, retain all substantive findings, links and context, and don't silently change its draft status, review state or scope.

Suggested flexible outline:

```markdown
# [Feature or area] — [Clear PR purpose]

> **Status:** Draft / Ready for review
> **Goal:** One-sentence explanation

## Overview
Brief context and outcome.

| Priority / Area | Finding or change | Status / Impact |
|---|---|---|
| 🔴 Critical | Short description | Needs fixing |

## 🔴 Critical findings
### 1. Descriptive issue title
**Location:** [Relevant source](link)

**Current behaviour:** ...
**Expected behaviour:** ...
**To resolve:** ...

## Verification
What was tested, what passed, and what still needs manual checking.

## Remaining work / notes
Known limitations, intentional differences and next steps.
```

For a small fix PR, shorten this outline rather than adding empty sections. For an audit, keep issue numbers stable across edits so discussions and links remain understandable.
