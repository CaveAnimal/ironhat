#!/usr/bin/env python3
"""Sync a manage_todo_list JSON snapshot into ironhat_TODO.md

Usage:
  python tools/sync_todos.py tools/manage_todo_list_snapshot.json

If no path is provided the script will look for
`tools/manage_todo_list_snapshot.json`.
"""
import json
import sys
from pathlib import Path
from datetime import datetime

SNAP = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("tools/manage_todo_list_snapshot.json")
OUT = Path("ironhat_TODO.md")

if not SNAP.exists():
    print("Snapshot file not found:", SNAP)
    sys.exit(1)

data = json.loads(SNAP.read_text())
# data expected: array of todos

lines = []
lines.append("```markdown")
lines.append("<!-- ironhat_TODO.md - Assistant-maintained todo list for project planning and actions -->")
lines.append("# Assistant TODOs (tracked by GitHub Copilot assistant)")
lines.append("")
lines.append("This file records the assistant's working todo items, their status, and short notes. The assistant will update this file as tasks are started, completed, or deferred so the project owner can see planned work.")
lines.append("")
lines.append("## Current Assistant Plan")
lines.append("")
for t in data:
    status = t.get('status','not-started')
    mark = "[ ]"
    if status == 'completed':
        mark = "[x]"
    elif status == 'in-progress':
        mark = "[-]"
    title = t.get('title','(no title)')
    desc = t.get('description','')
    # single-line description
    desc_line = desc.replace('\n',' ')[:300]
    lines.append(f"- {mark} {title} -- {desc_line}")

lines.append("")
lines.append("Last synced: " + datetime.utcnow().isoformat() + "Z")
lines.append("")
lines.append("```")

OUT.write_text('\n'.join(lines))
print("Wrote", OUT)
