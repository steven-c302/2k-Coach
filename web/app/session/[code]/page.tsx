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
  version: number;
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

export default function LiveSessionPage({ params }: { params: { code: string } }) {
  const { code } = params;
  const role = (useSearchParams().get("role") as Role) ?? "GUEST";

  const [connected, setConnected] = useState(false);
  const [state, setState] = useState<SessionStateResponse | null>(null);
  const [clientId, setClientId] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

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

      <p style={{ color: "#888", marginTop: "2rem", fontSize: "0.85rem" }}>
        Share code <strong>{code}</strong> with someone else and have them join as guest to see
        this update live in both tabs.
      </p>
    </main>
  );
}
