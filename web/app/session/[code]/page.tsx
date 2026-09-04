"use client";

import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Client } from "@stomp/stompjs";

const CORE_WS_URL = process.env.NEXT_PUBLIC_CORE_WS_URL ?? "ws://localhost:8080/ws";

type Role = "HOST" | "GUEST";

type SessionStateResponse = {
  code: string;
  status: "LOBBY" | "ACTIVE";
  hostClientId: string | null;
  guestClientId: string | null;
  hostReady: boolean;
  guestReady: boolean;
  lastNarration: string | null;
  version: number;
};

type CoachingLogEntry = {
  sessionCode: string;
  sequenceNumber: number;
  requestedAt: string;
  respondedAt: string;
  ruleEngineOutput: string;
  llmNarration: string;
  status: "DELIVERED" | "DISCARDED_STALE";
};

function getOrCreateClientId(code: string): string {
  const key = `nba2k-assistant:client-id:${code}`;
  let id = sessionStorage.getItem(key);
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(key, id);
  }
  return id;
}

function parseIds(input: string): number[] {
  return input
    .split(",")
    .map((s) => Number(s.trim()))
    .filter((n) => Number.isFinite(n) && n > 0);
}

export default function LiveSessionPage({ params }: { params: { code: string } }) {
  const { code } = params;
  const role = (useSearchParams().get("role") as Role) ?? "GUEST";

  const [connected, setConnected] = useState(false);
  const [state, setState] = useState<SessionStateResponse | null>(null);
  const [clientId, setClientId] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  const [teamAIds, setTeamAIds] = useState("");
  const [teamBIds, setTeamBIds] = useState("");
  const [analyzing, setAnalyzing] = useState(false);
  const [coachingLog, setCoachingLog] = useState<CoachingLogEntry[]>([]);

  useEffect(() => {
    const id = getOrCreateClientId(code);
    setClientId(id);

    const client = new Client({
      brokerURL: CORE_WS_URL,
      reconnectDelay: 2000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/sessions/${code}`, (message) => {
          setState(JSON.parse(message.body));
        });
        client.publish({
          destination: `/app/sessions/${code}/join`,
          body: JSON.stringify({ clientId: id, role }),
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [code]);

  function toggleReady() {
    if (!clientRef.current || !clientId || !state) return;
    const isHost = state.hostClientId === clientId;
    const currentlyReady = isHost ? state.hostReady : state.guestReady;
    clientRef.current.publish({
      destination: `/app/sessions/${code}/ready`,
      body: JSON.stringify({ clientId, ready: !currentlyReady }),
    });
  }

  function analyzeMatchup() {
    const teamAPlayerIds = parseIds(teamAIds);
    const teamBPlayerIds = parseIds(teamBIds);
    if (!clientRef.current || teamAPlayerIds.length === 0 || teamBPlayerIds.length === 0) return;

    setAnalyzing(true);
    clientRef.current.publish({
      destination: `/app/sessions/${code}/analyze`,
      body: JSON.stringify({ teamAPlayerIds, teamBPlayerIds }),
    });
    // Narration arrives async over the /topic subscription (or gets discarded as
    // stale) — this just clears the button's busy state after a moment rather
    // than tracking the exact response, since discards never broadcast anything.
    setTimeout(() => setAnalyzing(false), 3000);
  }

  async function refreshCoachingLog() {
    const response = await fetch(`/api/sessions/${code}/coaching-log`);
    if (response.ok) {
      const entries: CoachingLogEntry[] = await response.json();
      entries.sort((a, b) => b.sequenceNumber - a.sequenceNumber);
      setCoachingLog(entries);
    }
  }

  const isHost = state?.hostClientId === clientId;
  const isGuest = state?.guestClientId === clientId;
  const myReady = isHost ? state?.hostReady : isGuest ? state?.guestReady : false;

  return (
    <main>
      <h1>
        Session <code>{code}</code>
      </h1>
      <p>
        <span className={`status-pill ${connected ? "ok" : "error"}`}>
          {connected ? "connected" : "connecting..."}
        </span>{" "}
        {state && <span className="status-pill ok">{state.status}</span>}
      </p>

      {state && (
        <div className="player-grid" style={{ marginTop: "1.5rem" }}>
          <div className="player-card">
            <div className="player-card-info">
              <div className="player-card-name">Host{isHost && " (you)"}</div>
              <div className="player-card-meta">
                {state.hostClientId ? (state.hostReady ? "Ready" : "Not ready") : "Waiting for host..."}
              </div>
            </div>
          </div>
          <div className="player-card">
            <div className="player-card-info">
              <div className="player-card-name">Guest{isGuest && " (you)"}</div>
              <div className="player-card-meta">
                {state.guestClientId ? (state.guestReady ? "Ready" : "Not ready") : "Waiting for guest..."}
              </div>
            </div>
          </div>
        </div>
      )}

      {(isHost || isGuest) && (
        <button type="button" onClick={toggleReady} style={{ marginTop: "1.5rem" }}>
          {myReady ? "Mark not ready" : "Mark ready"}
        </button>
      )}

      <fieldset className="builder-form" style={{ marginTop: "2rem", maxWidth: 480 }}>
        <legend>Analyze a matchup (Milestone 6 — async/versioned coaching narration)</legend>
        <div className="field-row">
          <label style={{ flex: 1 }}>
            Team A player ids (comma-separated)
            <input type="text" placeholder="5,6,13,21,43" value={teamAIds} onChange={(e) => setTeamAIds(e.target.value)} />
          </label>
          <label style={{ flex: 1 }}>
            Team B player ids
            <input type="text" placeholder="222,232,233,236,242" value={teamBIds} onChange={(e) => setTeamBIds(e.target.value)} />
          </label>
        </div>
        <button type="button" onClick={analyzeMatchup} disabled={analyzing}>
          {analyzing ? "Analyzing..." : "Analyze matchup"}
        </button>
        {state?.lastNarration && (
          <p style={{ fontStyle: "italic", marginTop: "0.5rem" }}>&ldquo;{state.lastNarration}&rdquo;</p>
        )}
      </fieldset>

      <div style={{ marginTop: "1.5rem" }}>
        <button type="button" onClick={refreshCoachingLog}>
          Refresh coaching log
        </button>
        {coachingLog.length > 0 && (
          <table style={{ marginTop: "0.75rem", width: "100%", fontSize: "0.85rem", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ textAlign: "left" }}>
                <th>Seq</th>
                <th>Status</th>
                <th>Narration</th>
              </tr>
            </thead>
            <tbody>
              {coachingLog.map((entry) => (
                <tr key={entry.sequenceNumber}>
                  <td>{entry.sequenceNumber}</td>
                  <td>
                    <span className={`status-pill ${entry.status === "DELIVERED" ? "ok" : "error"}`}>
                      {entry.status}
                    </span>
                  </td>
                  <td>{entry.llmNarration}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <p style={{ color: "#888", marginTop: "2rem", fontSize: "0.85rem" }}>
        Share code <strong>{code}</strong> with someone else and have them join as guest to see
        this update live in both tabs.
      </p>
    </main>
  );
}
