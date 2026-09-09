"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function SessionLandingPage() {
  const router = useRouter();
  const [joinCode, setJoinCode] = useState("");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function createSession() {
    setCreating(true);
    setError(null);
    try {
      const response = await fetch("/api/sessions", { method: "POST" });
      const body = await response.json();
      if (!response.ok) throw new Error(body.error ?? "Failed to create session");
      router.push(`/session/${body.code}?role=HOST`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      setCreating(false);
    }
  }

  function joinSession(e: React.FormEvent) {
    e.preventDefault();
    if (joinCode.trim().length === 0) return;
    router.push(`/session/${joinCode.trim().toUpperCase()}?role=GUEST`);
  }

  return (
    <main>
      <h1>Live Session</h1>
      <p style={{ color: "var(--text-muted)" }}>
        Host or join a session to co-watch a matchup live — pick two teams, get instant rules-based
        mismatches plus an AI coach's take, broadcast to everyone in the session as it updates.
      </p>

      <div className="builder-form" style={{ maxWidth: 360 }}>
        <button type="button" onClick={createSession} disabled={creating}>
          {creating ? "Creating..." : "Host a new session"}
        </button>

        <form onSubmit={joinSession} className="field-row">
          <label style={{ flex: 1 }}>
            Join with a code
            <input
              type="text"
              placeholder="e.g. K3F9QZ"
              value={joinCode}
              onChange={(e) => setJoinCode(e.target.value)}
            />
          </label>
          <button type="submit" style={{ alignSelf: "flex-end" }}>
            Join
          </button>
        </form>
      </div>

      {error && <p className="status-pill error">{error}</p>}
    </main>
  );
}
