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
from datetime import datetime, timedelta, timezone
try:
    # Python 3.9+: use zoneinfo to get Olson tz database names
    from zoneinfo import ZoneInfo
except Exception:
    ZoneInfo = None

# Allow an optional --actor argument after the snapshot path so the changelog can record who ran the sync.
# Usage: python tools/sync_todos.py [snapshot-path] [--actor <name>]
actor = None
if len(sys.argv) > 1 and sys.argv[1] in ("--actor", "-a"):
    # called like: sync_todos.py --actor assistant
    actor = sys.argv[2] if len(sys.argv) > 2 else None
    SNAP = Path("tools/manage_todo_list_snapshot.json")
else:
    SNAP = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("tools/manage_todo_list_snapshot.json")
    # handle optional actor after the snapshot path
    if len(sys.argv) > 2 and sys.argv[2] in ("--actor", "-a"):
        actor = sys.argv[3] if len(sys.argv) > 3 else None

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
#!/usr/bin/env python3
"""Sync a manage_todo_list JSON snapshot into `ironhat_TODO.md`.

Usage:
  python tools/sync_todos.py [SNAPSHOT_PATH] [--actor NAME]

If no snapshot path is provided the script will look for
`tools/manage_todo_list_snapshot.json`.

The expected snapshot is a JSON array of todo objects with at least the keys:
  - id (number)
  - title (string)
  - description (string)
  - status (one of "not-started", "in-progress", "completed")

This script writes `ironhat_TODO.md` atomically (write to a temp file then rename).
"""

import argparse
import json
import sys
from pathlib import Path
from datetime import datetime, timedelta, timezone
from typing import Optional, List

try:
    # Python 3.9+: use zoneinfo for Olson tz database names
    from zoneinfo import ZoneInfo
except Exception:
    ZoneInfo = None


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Sync manage_todo_list snapshot into ironhat_TODO.md")
    p.add_argument("snapshot", nargs="?", default="tools/manage_todo_list_snapshot.json",
                   help="Path to snapshot JSON (default: tools/manage_todo_list_snapshot.json)")
    p.add_argument("--actor", "-a", help="Name of the actor running the sync (recorded in the file)")
    return p.parse_args()


def _central_time_str() -> str:
    """Return a formatted Central Time string with AM/PM and TZ abbreviation.

    Attempts to use zoneinfo if available. If not, falls back to an approximate
    US DST-aware conversion based on year, second Sunday in March -> first Sunday in November.
    """
    # Try zoneinfo first
    if ZoneInfo is not None:
        try:
            ct = datetime.now(ZoneInfo("America/Chicago"))
            return ct.strftime("%Y-%m-%d %I:%M:%S %p %Z")
        except Exception:
            pass

    # Fallback: compute from UTC and US DST rules (use timezone-aware UTC)
    utc = datetime.now(timezone.utc)

    def nth_weekday_of_month(year: int, month: int, weekday: int, n: int) -> datetime:
        # weekday: Monday=0 .. Sunday=6
        d = datetime(year, month, 1, tzinfo=timezone.utc)
        first_weekday = d.weekday()
        delta = (weekday - first_weekday) % 7
        day = 1 + delta + (n - 1) * 7
        return datetime(year, month, day, tzinfo=timezone.utc)

    year = utc.year
    # DST start: second Sunday in March at 02:00 local
    dst_start_local = nth_weekday_of_month(year, 3, 6, 2).replace(hour=2, minute=0, second=0, microsecond=0)
    # DST end: first Sunday in November at 02:00 local
    dst_end_local = nth_weekday_of_month(year, 11, 6, 1).replace(hour=2, minute=0, second=0, microsecond=0)

    # Compute local time assuming CST (-6)
    # local_assumed is UTC shifted by -6 hours (naive approach used previously)
    local_assumed = utc + timedelta(hours=-6)
    is_dst = False
    try:
        if dst_start_local <= local_assumed < dst_end_local:
            is_dst = True
    except Exception:
        is_dst = False

    offset_hours = -5 if is_dst else -6
    abbrev = "CDT" if is_dst else "CST"
    ct = (utc + timedelta(hours=offset_hours)).astimezone(timezone(timedelta(hours=offset_hours)))
    return ct.strftime(f"%Y-%m-%d %I:%M:%S %p {abbrev}")


def status_mark(status: str) -> str:
    s = (status or "").lower()
    if s == "completed":
        return "[x]"
    if s == "in-progress":
        return "[-]"
    return "[ ]"


def render_markdown(todos: List[dict], actor: Optional[str]) -> str:
    lines: List[str] = []
    lines.append("```markdown")
    lines.append("<!-- ironhat_TODO.md - Assistant-maintained todo list for project planning and actions -->")
    lines.append("# Assistant TODOs (tracked by GitHub Copilot assistant)")
    lines.append("")
    lines.append("This file records the assistant's working todo items, their status, and short notes. The assistant will update this file as tasks are started, completed, or deferred so the project owner can see planned work.")
    lines.append("")
    lines.append("## Current Assistant Plan")
    lines.append("")

    for t in todos:
        title = t.get("title", "(no title)")
        desc = t.get("description", "").replace("\n", " ")
        mark = status_mark(t.get("status", "not-started"))
        lines.append(f"- {mark} {title} -- {desc[:350]}")

    lines.append("")
    actor_line = actor if actor is not None else "assistant"
    lines.append("Last synced: " + _central_time_str() + f" — ran by {actor_line}")
    lines.append("")
    lines.append("```")
    return "\n".join(lines)


def main() -> None:
    args = parse_args()
    snap = Path(args.snapshot)
    out = Path("ironhat_TODO.md")

    if not snap.exists():
        print(f"Snapshot file not found: {snap}")
        sys.exit(2)

    try:
        data = json.loads(snap.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"Failed to read/parse snapshot JSON: {e}")
        sys.exit(3)

    if not isinstance(data, list):
        print("Snapshot JSON must be an array of todo objects")
        sys.exit(4)

    md = render_markdown(data, args.actor)

    # Atomic write: write to temporary then rename
    tmp = out.with_suffix(".tmp")
    tmp.write_text(md, encoding="utf-8")
    tmp.replace(out)
    print("Wrote", out)


if __name__ == "__main__":
    main()
        # Atomic write: write to temporary then rename
