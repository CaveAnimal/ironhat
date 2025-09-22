Read ironhat_PRD_v01.md, ironhat_TASKS_v01.md, and ironhat_EXAMPLES_v01.md
Follow the plan laid out in those documents
Do the tasks in the order they are listed
Mark each tak complete if it is complete or deferred if it is deferred
If a task is deffered edit the TASKS document to reflect the change including why it was deffered
Use the examples as a guide
If you have questions, ask them until you are 90% confident of success.
If you are unsure about something, ask for clarification.
If you make a mistake, own it, learn from it, record it in `ironhat_LESSONS_LEARNED.md` and move on.

Whenever you add/update your internal todo list, the assistant must immediately run the todo sync and update `ironhat_TODO.md`.

Required behavior for the assistant:

- Immediately after any change to the assistant-managed todo list (create/update/complete/defer), run the sync script to mirror the updated todo list into `ironhat_TODO.md`.
	- Preferred: invoke the Python helper `tools/sync_todos.py` from the repo root.
	- Windows-friendly: invoke the PowerShell wrapper `tools/sync_todos.ps1`.

- The assistant must also insert (or update) a single-line changelog at the top of `ironhat_TODO.md` in US Central Time with the format:

	Last synced: YYYY-MM-DD hh:mm:ss AM/PM CDT — ran by <actor>

	Where `<actor>` is the name of the user or assistant that initiated the sync (for assistant runs use `assistant`).

This rule is mandatory: the assistant will not finish any interaction in which it changed its internal todo list without running the sync and writing the changelog line.

Assistant responsibility for syncing todos
-----------------------------------------
- The assistant (this agent) is explicitly responsible for mirroring its internal `manage_todo_list` state into the on-disk file `ironhat_TODO.md`.
- Immediately after any change to the internal todo list (create/update/complete/defer), the assistant must:
	1. Run the todo sync helper (`python tools/sync_todos.py` or `tools/sync_todos.ps1`) from the repository root.
	2. Verify `ironhat_TODO.md` was written and that the top-line changelog shows the current US Central Time and `— ran by assistant` as the actor.
	3. If the sync fails or the file was not updated, the assistant must retry and surface the error in its message until the sync succeeds.

This requirement means the assistant owns the fidelity of the on-disk todo mirror and will keep `ironhat_TODO.md` up-to-date with its internal plan without user intervention.
 
ToDo file requirements
----------------------
- The project maintains a dedicated assistant-managed on-disk todo file: `ironhat_TODO.md`.
- Every time the assistant writes or updates `ironhat_TODO.md`, the file MUST include a single-line changelog at the top of the file with the current timestamp (in US Central Time) and the actor who performed the sync.
- The changelog line MUST use this exact format (24-hour vs AM/PM may be used but the example below is preferred):

		Last synced: YYYY-MM-DD hh:mm:ss AM/PM CDT — ran by <actor>

	- `<actor>` must be `assistant` when the assistant performed the sync, or a user name when a human ran the sync.
	- The timestamp must reflect the wall-clock time at which the file was actually written and must be updated on every sync operation (no stale timestamps allowed).
	- The assistant must NOT fabricate or reuse an earlier timestamp; always use the current time at the moment of writing.

- Implementation notes for the assistant:
	- Preferred invocation: `python tools/sync_todos.py` from the repository root.
	- Windows-friendly invocation: use the PowerShell wrapper `tools/sync_todos.ps1` which performs the same operation.
	- The assistant should capture and log the success message printed by the wrapper/script and ensure the changelog line was written into `ironhat_TODO.md` before finishing a message that modified its internal todo list.

Test environment note
---------------------
- This repository and its automated tests use an embedded H2 database (in-memory or file-backed) for testing and development. There is no PostgreSQL test environment provisioned in the workspace by default. Any features that rely on Postgres-specific behavior (for example, advisory locks) are implemented as best-effort fallbacks and will not be exercised in the standard test suite unless a Postgres instance is explicitly provided and configured.

Project scope note
------------------
- This project is currently developed as a desktop application. By default, there is no expectation to provision Docker containers or a Postgres server for development or CI. Any work requiring Docker or Postgres must be explicitly requested and added as an optional integration rather than a required part of development.
 
New mandatory behavior (assistant-run sync on messages)
----------------------------------------------------
- The assistant must run the todo sync and update `ironhat_TODO.md` at the end of every assistant message that posts project-related information, changes the project state, or could affect the todo list's interpretation. This must be performed even if the assistant did not modify the internal todo list in that message; the goal is to keep the on-disk `ironhat_TODO.md` strictly in sync with assistant communications.
- Preferred: invoke `python tools/sync_todos.py` from the repo root. Windows-friendly: use `tools/sync_todos.ps1`.

When you are at a point where you have several options moving forward, 
prioritize the Tasks and Todos in the order in which they appear in their respective documents.
If you have multiple options for how to proceed with a task or project,
create a decision matrix in ironhat_DECISION_MATRIX.md to help you decide.
When I post the message "[INFO] BUILD SUCCESS", that means that I ran the maven build and it was successful. and that you should continue
When I post the message "[INFO] BUILD FAILURE" Please refer to E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001\target\surefire-reports for the individual test results.
Always update `ironhat_POM_UPDATES.md` whenever a change is made to the pom.xml file.
Always read `ironhat_POM_UPDATES.md` to see the history of changes made to `pom.xml` and the rationale for each change.

Always read `ironhat_LESSONS_LEARNED.md` after `ironhat_POM_UPDATES.md` so lessons from dependency resolution and build work are visible when following the rules.

