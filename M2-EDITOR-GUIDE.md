# M2 Editor Guide

## Find and replace

Tap `⌕` in the top bar. The second row contains:

- `Aa` — match case
- `W` — whole word
- `.*` — regular expression
- `Replace` — replace the current match
- `All` — replace every match

Search is performed off the UI thread. Invalid regex input is reported without crashing the editor.

## Multi-occurrence editing

Select a repeated identifier or place the cursor inside it. Open the overflow menu and choose **Edit all occurrences**. Edits inside the primary highlighted occurrence are propagated to every highlighted occurrence. Use **Stop multi-edit** before making unrelated edits.

## Folding

Place the cursor inside a multi-line brace block and choose **Fold block**. The source text is not deleted; spans only collapse its visual lines. Any content edit safely clears stale folds. Use **Unfold all** to restore every block.

## Navigation

- **Go to line** jumps to a numeric line.
- **Go to symbol** lists supported classes, functions, methods, properties, headings, selectors, tags or declarations.
- Tap or drag the minimap to move vertically.

## Hardware keyboard shortcuts

- `Ctrl+S` — Save
- `Ctrl+F` — Find and replace
- `Ctrl+G` — Go to line
- `Ctrl+P` — Go to symbol
- `Ctrl+D` — Start/stop multi-occurrence edit
- `Ctrl+W` — Close active tab
