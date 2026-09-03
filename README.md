# Incident Manager Agent

Incident Manager Agent is a local Windows desktop client for monitoring a shared Outlook incident mailbox, matching incoming security emails to local runbooks, executing safe automation steps, escalating unresolved items to Teams, and showing a local React dashboard.

The app is intentionally local-first: each installed user configures their own mailbox details, Teams webhook, polling interval, and runbook folder from the dashboard.

## Features

- Windows desktop client packaged with `jpackage`
- Spring Boot backend running locally on `http://localhost:8080`
- React dashboard with incident filters for today, last 7 days, last month, custom range, and all data
- Incident breakdowns by severity, type, and outcome
- Settings UI for mailbox mode, Microsoft Graph details, Teams webhook, polling interval, and runbook folder
- Local SQLite audit database
- YAML runbooks stored in folders by incident type
- Embedded starter runbooks so first launch works even before users add their own
- Portable ZIP installer fallback when WiX is not available

## Repository layout

```text
.
├─ config\
│  ├─ application-local.example.yml
│  └─ application-local.yml              # local only, ignored by git
├─ runbooks\
│  ├─ phishing\
│  ├─ malware-alert\
│  └─ generic-security-alert\
├─ scripts\
│  ├─ build-ui.ps1
│  ├─ build-installer.ps1
│  └─ install-prerequisites.ps1
├─ src\main\java\com\incidentmanager\agent\
│  ├─ audit\
│  ├─ config\
│  ├─ desktop\
│  ├─ mailbox\
│  ├─ processor\
│  ├─ reporting\
│  ├─ runbook\
│  ├─ scheduler\
│  ├─ teams\
│  └─ web\
├─ src\main\resources\
│  ├─ application.yml
│  ├─ runbooks\                         # embedded starter runbooks
│  └─ static\                           # built React dashboard
├─ ui\                                  # React frontend source
├─ pom.xml
├─ run-agent.cmd
└─ run-agent.ps1
```

## Runtime data locations

Installed users get local, per-user data here:

```text
%USERPROFILE%\IncidentManagerAgent\
├─ config\application-local.yml
├─ incident-agent.db
├─ logs\agent.log
└─ runbooks\
```

Do not commit real `application-local.yml`, database files, logs, or secrets.

## How to run locally as a developer

Prerequisites:

- JDK 21
- Node.js LTS
- Maven 3.9+

From the project folder:

```powershell
cd "C:\Users\ranjasharma\OneDrive - Microsoft\Desktop\Incident Manager"
copy config\application-local.example.yml config\application-local.yml
powershell -ExecutionPolicy Bypass -File .\scripts\build-ui.ps1
mvn spring-boot:run
```

Open the dashboard:

```text
http://localhost:8080
```

Useful API checks:

```powershell
curl.exe http://localhost:8080/api/status
curl.exe "http://localhost:8080/api/incidents?range=today"
```

## How end users install and run the desktop client

Use the generated package:

```text
dist\IncidentManagerAgent.zip
```

Give users the ZIP file. They do not need Java, Maven, Node.js, or the source code.

Fastest option:

1. Extract `IncidentManagerAgent.zip`.
2. Double-click `Run-IncidentManagerAgent.cmd`.
3. The dashboard opens at `http://localhost:8080`.
4. Configure mailbox and Teams settings in the dashboard.
5. Put custom runbooks under `%USERPROFILE%\IncidentManagerAgent\runbooks\`.

Optional install option:

1. Extract `IncidentManagerAgent.zip`.
2. Double-click `Install-IncidentManagerAgent.cmd`.
3. The installer copies the app to `%LOCALAPPDATA%\IncidentManagerAgent`.
4. It creates a desktop shortcut named **Incident Manager Agent**.
5. It starts the agent and opens the dashboard.

If WiX Toolset is installed on the build machine, `scripts\build-installer.ps1` can also produce a Windows `.exe` installer. Without WiX, the ZIP installer is the supported fallback.

## How to build a distributable package

Install prerequisites:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-prerequisites.ps1
```

Build frontend, backend, tests, and package:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-installer.ps1
```

The build script downloads a local Maven copy into `tools\` if Maven is not already present there. `tools\` is ignored by git.

Outputs:

```text
dist\IncidentManagerAgent.zip
dist\Run-IncidentManagerAgent.cmd
dist\Install-IncidentManagerAgent.cmd
dist\README-FIRST.txt
dist\IncidentManagerAgent\
```

If WiX is available:

```text
dist\IncidentManagerAgent-0.1.0.exe
```

## How to open the dashboard

Start the desktop app, then open:

```text
http://localhost:8080
```

The dashboard shows:

- total incidents
- resolved by agent
- human involvement / escalated incidents
- unresolved incidents
- severity mix
- incident type mix
- detailed agent report per incident
- configurable date, status, severity, and search filters
- mailbox/runbook/Teams settings

## Mailbox configuration

The agent supports:

- `sample` mode for local demo/testing
- `graph` mode for Microsoft Graph mailbox monitoring

Configure from the dashboard or edit:

```text
%USERPROFILE%\IncidentManagerAgent\config\application-local.yml
```

Example:

```yaml
agent:
  mailbox:
    mode: graph
    poll-interval-ms: 60000
    graph:
      tenant-id: "<tenant-id>"
      client-id: "<app-client-id>"
      client-secret: "<app-client-secret>"
      mailbox-address: "shared-incident-mailbox@contoso.com"
  teams:
    webhook-url: "<teams-incoming-webhook-url>"
  runbook-directory: "%USERPROFILE%/IncidentManagerAgent/runbooks"
```

Security note: do not store normal mailbox user passwords. Use Microsoft Graph app registration or a future delegated OAuth flow.

## Runbooks

Runbooks are YAML files grouped by incident type:

```text
runbooks\
├─ phishing\
│  ├─ runbook.yml
│  └─ guidance.md
├─ malware-alert\
│  ├─ runbook.yml
│  └─ guidance.md
└─ generic-security-alert\
   ├─ runbook.yml
   └─ guidance.md
```

Only `runbook.yml` is executable. `guidance.md` is for humans.

Example action steps:

```yaml
steps:
  - name: Extract indicators
    action: parse_email
  - name: Record triage note
    action: add_note
    parameters:
      note: Check URLs, sender domain, affected users, and whether credentials were entered.
  - name: Escalate unresolved phishing
    action: teams_alert
    condition: unresolved
```

Supported MVP actions:

| Action | What it does |
|---|---|
| `parse_email` | Records sender, subject, and received time |
| `add_note` | Adds a runbook note to the incident report |
| `teams_alert` | Sends an alert through the configured Teams webhook |
| `mark_resolved` | Marks an incident as resolved when a runbook explicitly says so |

## Backend development

Backend code is under:

```text
src\main\java\com\incidentmanager\agent
```

Key areas:

| Package | Purpose |
|---|---|
| `mailbox` | Mailbox polling abstraction, sample mode, Graph mode |
| `runbook` | YAML loading, matching, severity calculation |
| `processor` | Incident processing and runbook execution |
| `audit` | SQLite entities/repositories |
| `teams` | Teams webhook escalation |
| `reporting` | Summary/reporting services |
| `web` | Dashboard APIs and settings APIs |
| `scheduler` | Mailbox polling job |

Run tests:

```powershell
mvn test
```

Build the backend JAR:

```powershell
mvn clean package
```

## Frontend development

Frontend source is under:

```text
ui\
```

Install dependencies:

```powershell
cd ui
npm install
```

Run frontend dev server:

```powershell
npm run dev
```

The Vite dev server proxies `/api` calls to `http://localhost:8080`.

Build frontend into Spring static resources:

```powershell
cd ..
powershell -ExecutionPolicy Bypass -File .\scripts\build-ui.ps1
```

The build output is written to:

```text
src\main\resources\static\
```

## Updating the app

To make backend changes:

1. Edit Java code under `src\main\java`.
2. Run `mvn test`.
3. Rebuild package with `scripts\build-installer.ps1`.
4. Publish the new ZIP or EXE.

To make frontend changes:

1. Edit `ui\src\main.tsx` or `ui\src\styles.css`.
2. Run `scripts\build-ui.ps1`.
3. Run `mvn test`.
4. Rebuild package with `scripts\build-installer.ps1`.

To add new runbook templates:

1. Add a folder under `runbooks\`.
2. Add `runbook.yml`.
3. Optionally add `guidance.md`.
4. Copy the same starter runbook into `src\main\resources\runbooks\` if it should be embedded for first launch.

## Uploading to GitHub

From this project folder:

```powershell
git init
git add .
git status
git commit -m "Initial Incident Manager Agent"
git branch -M main
git remote add origin https://github.com/<your-org-or-user>/<repo-name>.git
git push -u origin main
```

Do not commit:

- `dist\`
- `target\`
- `tools\`
- `ui\node_modules\`
- `config\application-local.yml`
- databases, logs, or secrets

For releases, upload `dist\IncidentManagerAgent.zip` or the generated `.exe` as GitHub Release assets rather than committing them to the repo.

## Current limitations

- The MVP uses explicit rule-based runbooks, not AI.
- Microsoft Graph support requires a valid app registration and permissions.
- Risky security actions should be added later behind approval gates.
- True `.exe` installer generation requires WiX Toolset on the build machine; otherwise use the ZIP installer.

## Safety model

The agent should automate low-risk triage, parsing, enrichment, audit logging, and escalation. Destructive actions such as disabling users, blocking URLs tenant-wide, deleting emails, or changing security policies should require explicit approval and strong audit logging.
