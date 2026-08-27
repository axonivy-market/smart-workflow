# Writing Documentation

House style for `doc/user/`. Follow it when adding or editing a page, so the set reads as one document rather than fourteen.

## Structure

The index lives in the repository root `README.md`. Below it, `doc/user/` is organized by what the reader is doing:

| Folder | Holds |
| --- | --- |
| _(root)_ | [Getting Started](../getting-started.md) and [Concepts](../concepts.md) |
| `build/` | Making an agent do something |
| `operate/` | Running agents safely and knowing what they did |
| `reference/` | Look-up tables — one owner per fact |
| `contribute/` | Extending Smart Workflow itself |

Add a page to the folder matching the reader's task, and list it in the [documentation index](https://github.com/axonivy-market/smart-workflow/blob/master/README.md) in the repository root. A page nothing links to does not exist.

## `doc/user/` must stay self-contained

This folder is the published documentation set — an external doc tool builds it on its own. A relative link that leaves it resolves inside the repository and breaks in the generated site.

- Links **inside** `doc/user/` are relative: `../reference/variables.md`, `agent-setup.md`.
- Links to **anything else** — source files, demo processes, `pom.xml`, `doc/dev/` — are absolute:
  `https://github.com/axonivy-market/smart-workflow/blob/master/…`
- Images live in `doc/user/img/` and are never referenced from outside the folder.

Nothing checks this automatically, and an escaping link still works on GitHub — so it is worth a deliberate look whenever you add one.

## One owner per fact

Before writing a fact down, check whether a reference page already owns it:

| Fact | Owner |
| --- | --- |
| What a provider supports | [Provider Capabilities](../reference/capabilities.md) |
| Any `AI.*` variable | [Variables](../reference/variables.md) |
| Any error code, and the problems that raise none | [Error Codes](../reference/error-codes.md) |
| Any public Java type | [Java API](../reference/java-api.md) |
| Agent element field semantics | [Agent Setup](../build/agent-setup.md) |

If it has an owner, **link — do not restate**. Duplicated facts drift, and the copy that goes stale is never the one you are reading.

## Page template

```text
# <Noun or task>

One paragraph: what this is and when you need it.

## Prerequisites        ← link, never repeat
## <the work>
## Example              ← exactly one, complete
## Common mistakes      ← real traps, not generic cautions
## See also
```

**Common mistakes** is not filler. Several problems do not raise an error, so the section that names them is the highest-value content on most pages. Prefer a real trap you have hit over a generic caution.

## Conventions

**Product name** — "Smart Workflow", two words, always. Never hyphenated, never translated, including in a translated page's H1.

**Spelling** — en-GB word choice (`defence`, `behaviour`, `customise`) with Oxford `-ize` endings (`organization`, `anonymized`). Add genuinely new terms to `.github/cspell-extra-words.txt`; do not add a word to silence an inconsistency you should fix instead.

**Headings** — sentence case: `## Handling a stopped agent`, not `## Handling A Stopped Agent`. Never skip a level; an `##` is always followed by `##` or `###`, never `####`.

**Emoji** — do not use them in headings or when naming a Designer UI group. Referring to a group as **Message** rather than **✉️ Message** keeps the docs correct when the UI icons change. Emoji also corrupt the generated anchor slug: `## 🎭 Mocking` becomes `#-mocking`, not `#mocking`.

**Field values** — a two-column table, not bold prose labels:

```markdown
| Field | Value |
| --- | --- |
| `Map result to:` | `in.result` |
```

**Tables** — spaced delimiter rows: `| --- | --- |`, or `| :---: |` to centre.

**Callouts** — `> **Important:**` for something that will bite the reader, `> **Note:**` for an aside. Use them sparingly; a page of callouts has no emphasis at all.

**Code blocks** — always tagged with a language (`yaml`, `java`, `text`, `markdown`). Untagged fences fail markdownlint.

## Before opening a PR

- [ ] the page is listed in the index in the repository root `README.md`
- [ ] no relative link leaves `doc/user/`, and every link target exists
- [ ] no fact duplicated from a reference page
- [ ] a **Common mistakes** section, if the topic has any
- [ ] headings sentence-cased, levels unskipped
- [ ] `README_DE.md` is a translation of an older revision — do not sync it by hand

## See also

- [Documentation index](https://github.com/axonivy-market/smart-workflow/blob/master/README.md) — the reading order this style serves
- [Chat Models](models.md) — the doc updates a new provider requires
