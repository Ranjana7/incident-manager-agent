import React from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";

type IncidentStatus = "RESOLVED" | "UNRESOLVED" | "ESCALATED";

type Incident = {
  id: number;
  providerMessageId: string;
  incidentType: string;
  severity: string;
  status: IncidentStatus;
  sender: string;
  subject: string;
  receivedAt: string;
  processedAt: string;
  report: string;
};

type IncidentResponse = {
  incidents: Incident[];
  total: number;
  bySeverity: Record<string, number>;
  byIncidentType: Record<string, number>;
  byStatus: Record<string, number>;
};

type Filters = {
  range: string;
  startDate: string;
  endDate: string;
  severity: string;
  status: string;
  incidentType: string;
  search: string;
};

type AgentSettings = {
  mailboxMode: string;
  pollIntervalMs: number;
  tenantId: string;
  clientId: string;
  clientSecret: string;
  mailboxAddress: string;
  teamsWebhookUrl: string;
  runbookDirectory: string;
  restartRequired: boolean;
};

const defaultFilters: Filters = {
  range: "today",
  startDate: "",
  endDate: "",
  severity: "",
  status: "",
  incidentType: "",
  search: ""
};

function App() {
  const [filters, setFilters] = React.useState<Filters>(defaultFilters);
  const [data, setData] = React.useState<IncidentResponse | null>(null);
  const [settings, setSettings] = React.useState<AgentSettings | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");
  const [settingsMessage, setSettingsMessage] = React.useState("");
  const [selected, setSelected] = React.useState<Incident | null>(null);

  const query = React.useMemo(() => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value);
    });
    return params.toString();
  }, [filters]);

  React.useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await fetch(`/api/incidents?${query}`);
        if (!response.ok) {
          throw new Error(`Dashboard API returned ${response.status}`);
        }
        const nextData = (await response.json()) as IncidentResponse;
        if (!cancelled) {
          setData(nextData);
          setSelected(nextData.incidents[0] ?? null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Unable to load incidents");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    const interval = window.setInterval(load, 30000);
    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [query]);

  React.useEffect(() => {
    async function loadSettings() {
      const response = await fetch("/api/settings");
      if (response.ok) {
        setSettings((await response.json()) as AgentSettings);
      }
    }
    loadSettings();
  }, []);

  async function saveSettings() {
    if (!settings) return;
    setSettingsMessage("");
    const response = await fetch("/api/settings", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(settings)
    });
    if (!response.ok) {
      setSettingsMessage(`Settings save failed: ${response.status}`);
      return;
    }
    setSettings((await response.json()) as AgentSettings);
    setSettingsMessage("Settings saved. Restart the desktop agent for mailbox/runbook changes to take effect.");
  }

  const incidents = data?.incidents ?? [];
  const resolved = data?.byStatus.RESOLVED ?? 0;
  const escalated = data?.byStatus.ESCALATED ?? 0;
  const unresolved = data?.byStatus.UNRESOLVED ?? 0;

  return (
    <main className="shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Desktop agent web dashboard</p>
          <h1>Incident Manager</h1>
          <p className="subtitle">
            Live mailbox triage view for agent-resolved incidents, escalations, and cases needing human involvement.
          </p>
        </div>
        <div className="hero-actions">
          <a className="button ghost" href="/api/installer">Download Windows agent</a>
          <button className="button" onClick={() => window.location.reload()}>Refresh</button>
        </div>
      </header>

      <section className="metrics">
        <Metric label="Total incidents" value={data?.total ?? 0} tone="neutral" />
        <Metric label="Resolved by agent" value={resolved} tone="success" />
        <Metric label="Human involvement" value={escalated + unresolved} tone="warning" />
        <Metric label="Unresolved" value={unresolved} tone="danger" />
      </section>

      <section className="panel filters">
        <Select label="Range" value={filters.range} onChange={(range) => setFilters({ ...filters, range })}>
          <option value="today">Today</option>
          <option value="last-week">Last 7 days</option>
          <option value="last-month">Last month</option>
          <option value="custom">Custom</option>
          <option value="all">All</option>
        </Select>
        <Field label="Start" type="date" value={filters.startDate} disabled={filters.range !== "custom"} onChange={(startDate) => setFilters({ ...filters, startDate })} />
        <Field label="End" type="date" value={filters.endDate} disabled={filters.range !== "custom"} onChange={(endDate) => setFilters({ ...filters, endDate })} />
        <Select label="Status" value={filters.status} onChange={(status) => setFilters({ ...filters, status })}>
          <option value="">All statuses</option>
          <option value="RESOLVED">Resolved by agent</option>
          <option value="ESCALATED">Human involvement</option>
          <option value="UNRESOLVED">Unresolved</option>
        </Select>
        <Select label="Severity" value={filters.severity} onChange={(severity) => setFilters({ ...filters, severity })}>
          <option value="">All severities</option>
          <option value="critical">Critical</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </Select>
        <Field label="Search" value={filters.search} placeholder="Subject, sender, report..." onChange={(search) => setFilters({ ...filters, search })} />
      </section>

      {error && <div className="error">{error}</div>}
      {loading && <div className="notice">Loading latest incidents...</div>}

      {settings && (
        <section className="panel settings">
          <div className="panel-heading">
            <div>
              <h2>Agent configuration</h2>
              <p className="muted">Configure the monitored mailbox for this machine. Use Graph app credentials or OAuth configuration, not a normal mailbox password.</p>
            </div>
            <button className="button" onClick={saveSettings}>Save settings</button>
          </div>
          <div className="settings-grid">
            <Select label="Mailbox mode" value={settings.mailboxMode} onChange={(mailboxMode) => setSettings({ ...settings, mailboxMode })}>
              <option value="sample">Sample/demo data</option>
              <option value="graph">Microsoft Graph mailbox</option>
            </Select>
            <Field label="Polling interval ms" type="number" value={String(settings.pollIntervalMs)} onChange={(pollIntervalMs) => setSettings({ ...settings, pollIntervalMs: Number(pollIntervalMs) })} />
            <Field label="Tenant ID" value={settings.tenantId} onChange={(tenantId) => setSettings({ ...settings, tenantId })} />
            <Field label="Client ID" value={settings.clientId} onChange={(clientId) => setSettings({ ...settings, clientId })} />
            <Field label="Client secret" type="password" value={settings.clientSecret} onChange={(clientSecret) => setSettings({ ...settings, clientSecret })} />
            <Field label="Mailbox address" value={settings.mailboxAddress} placeholder="shared-mailbox@contoso.com" onChange={(mailboxAddress) => setSettings({ ...settings, mailboxAddress })} />
            <Field label="Teams webhook URL" value={settings.teamsWebhookUrl} onChange={(teamsWebhookUrl) => setSettings({ ...settings, teamsWebhookUrl })} />
            <Field label="Runbook folder" value={settings.runbookDirectory} onChange={(runbookDirectory) => setSettings({ ...settings, runbookDirectory })} />
          </div>
          {settingsMessage && <p className="notice compact">{settingsMessage}</p>}
        </section>
      )}

      <section className="content-grid">
        <div className="panel incident-list">
          <div className="panel-heading">
            <h2>Incidents</h2>
            <span>{incidents.length} shown</span>
          </div>
          <div className="table">
            {incidents.map((incident) => (
              <button
                key={incident.id}
                className={`row ${selected?.id === incident.id ? "selected" : ""}`}
                onClick={() => setSelected(incident)}
              >
                <span className={`severity ${incident.severity}`}>{incident.severity}</span>
                <span>
                  <strong>{incident.subject}</strong>
                  <small>{incident.sender}</small>
                </span>
                <span className={`status ${incident.status.toLowerCase()}`}>{statusLabel(incident.status)}</span>
              </button>
            ))}
            {incidents.length === 0 && <div className="empty">No incidents match the selected filters.</div>}
          </div>
        </div>

        <aside className="panel detail">
          <div className="panel-heading">
            <h2>Incident detail</h2>
            {selected && <span>{formatDate(selected.processedAt)}</span>}
          </div>
          {selected ? (
            <>
              <h3>{selected.subject}</h3>
              <dl>
                <dt>Status</dt><dd>{statusLabel(selected.status)}</dd>
                <dt>Severity</dt><dd>{selected.severity}</dd>
                <dt>Type</dt><dd>{selected.incidentType}</dd>
                <dt>Sender</dt><dd>{selected.sender}</dd>
                <dt>Received</dt><dd>{formatDate(selected.receivedAt)}</dd>
              </dl>
              <pre>{selected.report}</pre>
            </>
          ) : (
            <div className="empty">Select an incident to inspect the agent report.</div>
          )}
        </aside>
      </section>

      <section className="panel breakdowns">
        <Breakdown title="Severity mix" values={data?.bySeverity ?? {}} />
        <Breakdown title="Incident types" values={data?.byIncidentType ?? {}} />
        <Breakdown title="Resolution outcome" values={data?.byStatus ?? {}} />
      </section>
    </main>
  );
}

function Metric({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <article className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function Field(props: { label: string; value: string; onChange: (value: string) => void; type?: string; placeholder?: string; disabled?: boolean }) {
  return (
    <label className="control">
      <span>{props.label}</span>
      <input type={props.type ?? "text"} value={props.value} placeholder={props.placeholder} disabled={props.disabled} onChange={(event) => props.onChange(event.target.value)} />
    </label>
  );
}

function Select(props: { label: string; value: string; onChange: (value: string) => void; children: React.ReactNode }) {
  return (
    <label className="control">
      <span>{props.label}</span>
      <select value={props.value} onChange={(event) => props.onChange(event.target.value)}>{props.children}</select>
    </label>
  );
}

function Breakdown({ title, values }: { title: string; values: Record<string, number> }) {
  const max = Math.max(1, ...Object.values(values));
  return (
    <div>
      <h2>{title}</h2>
      {Object.entries(values).map(([key, value]) => (
        <div className="bar-row" key={key}>
          <span>{key}</span>
          <div className="bar"><i style={{ width: `${(value / max) * 100}%` }} /></div>
          <strong>{value}</strong>
        </div>
      ))}
      {Object.keys(values).length === 0 && <p className="muted">No data yet.</p>}
    </div>
  );
}

function statusLabel(status: IncidentStatus) {
  if (status === "RESOLVED") return "Resolved by agent";
  if (status === "ESCALATED") return "Human involvement";
  return "Unresolved";
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(<App />);
