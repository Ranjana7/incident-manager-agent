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

## 3. What to configure in the dashboard

Open:

```text
http://localhost:8080
```

Find the `Agent configuration` section.

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
