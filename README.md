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

Fastest local desktop option:

1. Extract `IncidentManagerAgent.zip`.
2. Double-click `Run-IncidentManagerAgent.cmd`.
3. The dashboard opens at `http://localhost:8080`.
4. Click **Agent setup** if you need to configure mailbox and Teams settings for this local desktop agent.
5. Put custom runbooks under `%USERPROFILE%\IncidentManagerAgent\runbooks\`.

Optional install option:

1. Extract `IncidentManagerAgent.zip`.
2. Double-click `Install-IncidentManagerAgent.cmd`.
3. The installer copies the app to `%LOCALAPPDATA%\IncidentManagerAgent`.
4. It creates a desktop shortcut named **Incident Manager Agent**.
5. It starts the agent and opens the dashboard.

If WiX Toolset is installed on the build machine, `scripts\build-installer.ps1` can also produce a Windows `.exe` installer. Without WiX, the ZIP installer is the supported fallback.

## Simple setup guide for non-technical users

Most users should not need to understand Java, Maven, Node.js, or the source code. They only need the packaged ZIP from a GitHub Release or another shared location.

The ZIP includes `README-FIRST.txt`, which contains step-by-step setup instructions for non-technical users. The same guide is tracked in this repo at:

```text
docs\END_USER_SETUP.md
```

After running `Run-IncidentManagerAgent.cmd`, open:

```text
http://localhost:8080
```

Click **Agent setup** to open the local desktop agent configuration panel.

The setup panel is intentionally hidden by default and is only shown for local dashboards such as `http://localhost:8080`. People viewing a shared hosted report dashboard should not see or need mailbox credentials, client secrets, or local runbook settings.

| Field | What to enter | Who usually provides it |
|---|---|---|
| Mailbox mode | Use `sample` for demo/testing. Use `graph` for real mailbox monitoring. | User or admin |
| Tenant ID | Microsoft Entra tenant ID for the organization. | IT/admin |
| Client ID | App registration client/application ID. | IT/admin |
| Client secret | Secret for the app registration. | IT/admin |
| Mailbox address | Shared incident mailbox to monitor, such as `incidents@contoso.com`. | User/team lead |
| Teams webhook URL | Incoming webhook URL for the Teams channel where escalations should go. Optional for demo. | Teams/channel owner |
| Polling interval | How often to check the mailbox, in milliseconds. Example: `60000` means every 60 seconds. | User/team lead |
| Runbook folder | Optional custom runbook folder. Leave as `runbooks` to use bundled/default runbooks. | User/team lead |

For demo or hackathon judging, users can leave mailbox mode as:

```text
sample
```

In `sample` mode, no Microsoft Graph details, mailbox address, or Teams webhook are required. The agent uses sample incidents and the embedded starter runbooks.

For real mailbox monitoring, users need help from an IT/admin person to create or provide a Microsoft Graph app registration with the right mailbox permissions. Do not ask normal users to type their personal mailbox password into this app.

### What if the user does not have Microsoft Graph details?

They can still run the app in `sample` mode and review the dashboard/runbook workflow. For production use, the deployment owner should provide a prepared configuration or an onboarding guide with:

- tenant ID
- client ID
- client secret
- mailbox address
- Teams webhook URL

### Are runbooks already included?

Yes. The app includes embedded starter runbooks for:

```text
phishing
malware-alert
generic-security-alert
```

Users can add custom runbooks later under:

```text
%USERPROFILE%\IncidentManagerAgent\runbooks\
```

If no custom runbooks exist, the app still starts using the embedded starter runbooks.

## Local desktop dashboard vs shared hosted dashboard

There are two possible ways to use this project.

| Mode | Who uses it | How it works |
|---|---|---|
| Local desktop agent | Analyst/operator monitoring a mailbox from their machine | User downloads the ZIP, runs the local agent, configures mailbox/runbooks/Teams in **Agent setup**, and opens `http://localhost:8080`. |
| Shared hosted dashboard | Managers, teammates, or report viewers who should not install the agent | Organization hosts the backend/dashboard on a server/domain. Viewers open a shared URL and see reports/incidents only. |

For shared report viewers, hide operational setup. They should not need:

- tenant ID
- client ID
- client secret
- mailbox address
- runbook folder path

Those values should be configured by the deployment owner/admin on the hosted backend.

## Hosting on your own domain

You can host the dashboard/backend centrally if you want reports to be shared with users who do not install the desktop agent.

Example custom domains:

```text
https://incidents.arictra.com
https://incident-manager.your-org.com
```

For a hosted deployment, you need:

| Requirement | Why it is needed |
|---|---|
| Hosting platform | Azure App Service, Azure Container Apps, VM, or internal server to run the Spring Boot backend |
| Custom domain | Example: `incidents.arictra.com` |
| HTTPS certificate | Required for safe browser access |
| Central database | Replace local SQLite with Azure SQL, PostgreSQL, or another shared database |
| Microsoft Graph app registration | Lets the hosted service monitor the shared mailbox |
| Secret storage | Store client secrets in Azure Key Vault or equivalent, not in files |
| Authentication | Use Microsoft Entra ID so only approved users can view incident reports |
| Teams integration | Webhook, Teams app, or bot for escalation alerts |

Important: the current local desktop MVP is not hardened for public anonymous hosting. Before exposing it on a real domain, add authentication, central database configuration, production secret storage, HTTPS, and access controls.

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
