"use client";

import { useState } from "react";

type TapEvent = { team: "A" | "B"; points: number };

/**
 * Manual fallback/complement to OCR (plan §6) — reuses pickup-stats's
 * interaction pattern (tap-to-increment, undo-by-popping-last-entry), posts
 * to the same event-shaped endpoint the OCR pipeline uses so both flow
 * through one unified stream into the session's live state.
 */
export default function TapTracker({ sessionCode }: { sessionCode: string }) {
  const [scoreA, setScoreA] = useState(0);
  const [scoreB, setScoreB] = useState(0);
  const [history, setHistory] = useState<TapEvent[]>([]);

  async function postScore(teamAScore: number, teamBScore: number) {
    await fetch("/api/vision/events", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sessionId: sessionCode,
        type: "SCORE_UPDATE",
        payload: { teamAScore, teamBScore },
        capturedAt: new Date().toISOString(),
      }),
    });
  }

  async function tap(team: "A" | "B", points: number) {
    const nextA = team === "A" ? scoreA + points : scoreA;
    const nextB = team === "B" ? scoreB + points : scoreB;
    setScoreA(nextA);
    setScoreB(nextB);
    setHistory((h) => [...h, { team, points }]);
    await postScore(nextA, nextB);
  }

  async function undo() {
    const last = history[history.length - 1];
    if (!last) return;
    const nextA = last.team === "A" ? scoreA - last.points : scoreA;
    const nextB = last.team === "B" ? scoreB - last.points : scoreB;
    setScoreA(nextA);
    setScoreB(nextB);
    setHistory((h) => h.slice(0, -1));
    await postScore(nextA, nextB);
  }

  return (
    <fieldset className="builder-form" style={{ maxWidth: 480 }}>
      <legend>Manual tap-tracker (fallback/complement to OCR)</legend>
      <div className="field-row">
        <div>
          <strong>Team A: {scoreA}</strong>
          <div className="field-row" style={{ marginTop: "0.4rem" }}>
            <button type="button" onClick={() => tap("A", 2)}>
              +2
            </button>
            <button type="button" onClick={() => tap("A", 3)}>
              +3
            </button>
          </div>
        </div>
        <div>
          <strong>Team B: {scoreB}</strong>
          <div className="field-row" style={{ marginTop: "0.4rem" }}>
            <button type="button" onClick={() => tap("B", 2)}>
              +2
            </button>
            <button type="button" onClick={() => tap("B", 3)}>
              +3
            </button>
          </div>
        </div>
      </div>
      <button type="button" onClick={undo} disabled={history.length === 0}>
        Undo last tap
      </button>
    </fieldset>
  );
}
