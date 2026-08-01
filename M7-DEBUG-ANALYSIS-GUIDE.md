# NovaIDE M7 — Debug & Analysis Guide

## Open the center

Open **⋮ → Debug & Analysis**. All M7 analysis is local and runs in a bounded background queue.

## Crash and ANR tracing

Choose **Analyze pasted crash / ANR trace** for clipboard output, or **Import crash, log or ZIP** for saved diagnostics. Keep the complete exception and `Caused by` chain. For ANRs, include the main-thread stack and timeout reason.

NovaIDE selects the deepest parsed exception as the probable root cause, marks project-owned stack frames with an arrow, resolves a workspace path where possible, creates a stable fingerprint, redacts likely credentials, and offers **Open suspected file**.

Android blocks normal applications from unrestricted Logcat access. M7 therefore analyzes only logs the user explicitly pastes or selects.

## Full project health audit

The audit shows a score, severity totals, duplicate groups, and dependency cycles. Open a category to review findings, then tap a finding to inspect its evidence/recommendation and open the exact file/line.

The score is a triage signal, not a release certificate. Critical and high findings receive larger penalties; info findings do not reduce the score.

## Duplicate analysis

Exact groups compare normalized whole-file content. Repeated blocks use bounded eight-line normalized windows and suppress overlapping windows from the same file. Review domain meaning before extracting shared code.

## Dead-code analysis

M7 deliberately labels candidates **possibly** unused. Reflection, dependency injection, serializers, JNI, manifests, resource lookup by name, tests, framework callbacks, product flavors, and generated references can make a candidate live even when a text reference is absent.

## Dependency map

The map resolves common Kotlin/Java packages, JavaScript/TypeScript modules, and Python imports. It lists high-degree hubs, disconnected sources, edges, and strongly connected components. Dynamic imports/reflection are not fully resolvable.

## Performance report

Checks include large source/assets, long functions, deep nesting, blocking calls, whole-file I/O, nested loops, repeated concatenation, full list refreshes, bitmap decoding, and unstructured coroutine scopes. Confirm actual hot paths with runtime measurement before large rewrites.

## Security report

Checks include sensitive paths, secret-like values, cleartext endpoints, weak hashes, AES/ECB, weak randomness, risky WebViews, TLS bypasses, wildcard CORS, dynamic execution, cleartext Android traffic, exported components, and unpinned dependencies. Values matching secret patterns are not displayed.

## Mobile safety limits

- 8,000 project files
- 700 KB maximum content read per eligible text file
- 2,000,000 crash-log characters
- 8,000 dependency edges
- 600 combined findings
- 120 duplicate groups
- Generated/vendor folders excluded
