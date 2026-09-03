# Incident Manager Agent - End User Setup

This guide is for people who downloaded `IncidentManagerAgent.zip`.

You do not need Java, Maven, Node.js, or developer tools. Everything needed to run the agent is included in the ZIP.

## 1. Start the agent

1. Extract `IncidentManagerAgent.zip`.
2. Double-click:

```text
Run-IncidentManagerAgent.cmd
```

3. The dashboard should open automatically.

If it does not open, go to:

```text
http://localhost:8080
```

## 2. Optional local install

Instead of running from the extracted folder, you can install it locally:

1. Extract `IncidentManagerAgent.zip`.
2. Double-click:

```text
Install-IncidentManagerAgent.cmd
```

3. The app is copied to:

```text
%LOCALAPPDATA%\IncidentManagerAgent
```

4. A desktop shortcut named `Incident Manager Agent` is created.

## 3. Where to configure the local desktop agent

Open:

```text
http://localhost:8080
```

Click:

```text
Agent setup
```

This opens the local desktop agent configuration panel.

Important: agent setup is only shown on the local desktop dashboard, for example `localhost`. If someone is viewing a shared hosted report dashboard on a company domain, they should not see or need mailbox/client-secret settings.

## 4. Demo mode

For demo/testing, use:

| Field | Value |
|---|---|
| Mailbox mode | `sample` |
| Polling interval ms | `60000` |
| Runbook folder | `runbooks` |

Leave these blank in demo mode:

```text
Tenant ID
Client ID
Client secret
Mailbox address
Teams webhook URL
```

In demo mode, the agent uses sample incidents and embedded starter runbooks.

## 5. Real mailbox mode

For real mailbox monitoring, set:

| Field | What to enter | Who should provide it |
|---|---|---|
| Mailbox mode | `graph` | User/team lead |
| Tenant ID | Microsoft Entra tenant ID | IT/admin |
| Client ID | Microsoft Entra app registration client/application ID | IT/admin |
| Client secret | Client secret for the app registration | IT/admin |
| Mailbox address | Shared incident mailbox, for example `incidents@contoso.com` | User/team lead |
| Teams webhook URL | Incoming webhook URL for the Teams channel | Teams/channel owner |
| Polling interval ms | `60000` for every 60 seconds | User/team lead |
| Runbook folder | Leave as `runbooks` unless using a custom folder | User/team lead |

Do not enter a normal mailbox password. Production setup should use Microsoft Graph app registration or a future delegated OAuth flow.

## 6. Runbooks

Starter runbooks are already included:

```text
phishing
malware-alert
generic-security-alert
```

To add your own runbooks, place them under:

```text
%USERPROFILE%\IncidentManagerAgent\runbooks\
```

Use this structure:

```text
%USERPROFILE%\IncidentManagerAgent\runbooks\
└─ your-incident-type\
   ├─ runbook.yml
   └─ guidance.md
```

Only `runbook.yml` is executable by the agent. `guidance.md` is for human instructions.

## 7. Where local files are stored

The app stores local data here:

```text
%USERPROFILE%\IncidentManagerAgent\
├─ config\application-local.yml
├─ incident-agent.db
├─ logs\agent.log
└─ runbooks\
```

## 8. Troubleshooting

If the dashboard does not open:

1. Wait 20-30 seconds.
2. Open `http://localhost:8080` manually.
3. Check the log file:

```text
%USERPROFILE%\IncidentManagerAgent\logs\agent.log
```

If port `8080` is already in use, close the other app or change the port in:

```text
%USERPROFILE%\IncidentManagerAgent\config\application-local.yml
```

Then restart Incident Manager Agent.

## 9. Shared hosted dashboard option

The ZIP runs a local desktop agent for one user/machine. If your organization wants a shared dashboard that anyone can open without installing the desktop agent, host the backend/dashboard centrally instead.

Example:

```text
https://incidents.arictra.com
```

or:

```text
https://incident-manager.your-org.com
```

For a hosted setup, users do not run `Run-IncidentManagerAgent.cmd`. Instead, they open the shared URL in a browser.

The deployment owner/admin must provide and configure:

- server or cloud hosting location, such as Azure App Service, Azure Container Apps, or a secured VM
- custom domain, such as `incidents.arictra.com`
- HTTPS certificate
- Microsoft Graph app registration
- mailbox address to monitor
- Teams webhook or Teams app/bot integration
- central database, such as Azure SQL, PostgreSQL, or another managed database
- authentication, ideally Microsoft Entra ID, so only approved users can view reports
- secret storage, such as Azure Key Vault

Do not expose the hosted dashboard publicly without authentication. Reports may contain security incident information.
