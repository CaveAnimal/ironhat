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
When you are at a point where you have several options moving forward, 
prioritize the Tasks and Todos in the order in which they appear in their respective documents.
If you have multiple options for how to proceed with a task or project,
create a decision matrix in ironhat_DECISION_MATRIX.md to help you decide.
When I post the message "[INFO] BUILD SUCCESS", that means that I ran the maven build and it was successful. and that you should continue
When I post the message "[INFO] BUILD FAILURE" Please refer to E:\MyProjects\MyGitHubCopilot\ironhat\FeH-001\target\surefire-reports for the individual test results.
Always update `ironhat_POM_UPDATES.md` whenever a change is made to the pom.xml file.
Always read `ironhat_POM_UPDATES.md` to see the history of changes made to `pom.xml` and the rationale for each change.

Always read `ironhat_LESSONS_LEARNED.md` after `ironhat_POM_UPDATES.md` so lessons from dependency resolution and build work are visible when following the rules.

