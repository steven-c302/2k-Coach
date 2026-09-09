"use client";

import { useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Client } from "@stomp/stompjs";
import TapTracker from "@/components/TapTracker";
import MultiPlayerPicker from "@/components/MultiPlayerPicker";
import type { MismatchResponse, PlayerSummary } from "@/lib/core-client";

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
  liveGameState: Record<string, unknown>;
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

function humanizeCategory(category: string): string {
  return category
    .toLowerCase()
    .split("_")
    .map((w) => w[0]?.toUpperCase() + w.slice(1))
    .join(" ");
}

function severityColor(severity: string): string {
  if (severity === "HIGH") return "var(--ovr-elite)";
  if (severity === "MEDIUM") return "var(--ovr-great)";
  return "var(--ovr-average)";
}

function MismatchCard({ mismatch }: { mismatch: MismatchResponse }) {
  return (
    <div className="player-card" style={{ alignItems: "flex-start" }}>
      <span className="ovr-badge" style={{ background: severityColor(mismatch.severity), flexShrink: 0 }}>
        {mismatch.severity}
      </span>
      <div className="player-card-info">
        <div className="player-card-name">
          {humanizeCategory(mismatch.category)} <span style={{ color: "var(--text-muted)", fontWeight: 500 }}>— favors {mismatch.favoredTeam}</span>
        </div>
        <div className="player-card-meta" style={{ marginTop: "0.3rem" }}>
          {mismatch.evidence}
        </div>
      </div>
    </div>
  );
}

export default function LiveSessionPage({ params }: { params: { code: string } }) {
  const { code } = params;
  const role = (useSearchParams().get("role") as Role) ?? "GUEST";

  const [connected, setConnected] = useState(false);
  const [state, setState] = useState<SessionStateResponse | null>(null);
  const [clientId, setClientId] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  const [teamAPlayers, setTeamAPlayers] = useState<PlayerSummary[]>([]);
  const [teamBPlayers, setTeamBPlayers] = useState<PlayerSummary[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const [analyzeError, setAnalyzeError] = useState<string | null>(null);
  const [mismatches, setMismatches] = useState<MismatchResponse[] | null>(null);
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

  async function analyzeMatchup() {
    const teamAPlayerIds = teamAPlayers.map((p) => p.id);
    const teamBPlayerIds = teamBPlayers.map((p) => p.id);
    if (!clientRef.current || teamAPlayerIds.length === 0 || teamBPlayerIds.length === 0) return;

    setAnalyzing(true);
    setAnalyzeError(null);
    setMismatches(null);

    // The rules engine is fast/synchronous - fetch it directly over REST so mismatches show up
    // immediately, instead of waiting on the LLM narration (which arrives later over the socket).
    try {
      const response = await fetch("/api/matchups/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ teamAPlayerIds, teamBPlayerIds }),
      });
      const body = await response.json();
      if (!response.ok) throw new Error(body.error ?? "Failed to analyze matchup");
      setMismatches(body.mismatches);
    } catch (err) {
      setAnalyzeError(err instanceof Error ? err.message : "Unknown error");
    }

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
  const canAnalyze = teamAPlayers.length > 0 && teamBPlayers.length > 0;

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

      <fieldset className="builder-form" style={{ marginTop: "2rem" }}>
        <legend>Analyze a matchup</legend>
        <div className="matchup-grid" style={{ marginTop: 0 }}>
          <MultiPlayerPicker label="Team A" selected={teamAPlayers} onChange={setTeamAPlayers} />
          <MultiPlayerPicker label="Team B" selected={teamBPlayers} onChange={setTeamBPlayers} />
        </div>
        <button type="button" onClick={analyzeMatchup} disabled={analyzing || !canAnalyze}>
          {analyzing ? "Analyzing..." : "Analyze matchup"}
        </button>

        {analyzeError && <p className="status-pill error">{analyzeError}</p>}

        {mismatches && (
          <div style={{ marginTop: "0.5rem" }}>
            <div className="section-label">
              {mismatches.length === 0 ? "No notable mismatches" : "Mismatches"}
            </div>
            {mismatches.length > 0 && (
              <div className="player-grid">
                {mismatches.map((m, i) => (
                  <MismatchCard key={i} mismatch={m} />
                ))}
              </div>
            )}
          </div>
        )}

        {state?.lastNarration && (
          <div style={{ marginTop: "0.75rem" }}>
            <div className="section-label">Coach's take</div>
            <p style={{ fontStyle: "italic" }}>&ldquo;{state.lastNarration}&rdquo;</p>
          </div>
        )}
      </fieldset>

      <div style={{ marginTop: "2rem" }}>
        <TapTracker sessionCode={code} />
      </div>

      {state && Object.keys(state.liveGameState).length > 0 && (
        <p style={{ marginTop: "1rem", fontSize: "0.85rem", color: "var(--text-muted)" }}>
          Live observed state (from OCR or the tap-tracker):{" "}
          {Object.entries(state.liveGameState)
            .map(([key, value]) => `${key}=${value}`)
            .join(", ")}
        </p>
      )}

      <div style={{ marginTop: "1.5rem" }}>
        <button type="button" onClick={refreshCoachingLog}>
          Refresh coaching log
        </button>
        {coachingLog.length > 0 && (
          <div style={{ overflowX: "auto" }}>
            <table style={{ marginTop: "0.75rem", width: "100%" }}>
              <thead>
                <tr>
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
          </div>
        )}
      </div>

      <p style={{ color: "var(--text-muted)", marginTop: "2rem", fontSize: "0.85rem" }}>
        Share code <strong>{code}</strong> with someone else and have them join as guest to see
        this update live in both tabs.
      </p>
    </main>
  );
}
