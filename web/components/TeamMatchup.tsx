"use client";

import { useState } from "react";
import type { GeneratedRoster, PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";
import { ovrColor } from "./PlayerCard";

function average(players: PlayerSummary[]): number {
  if (players.length === 0) return 0;
  return Math.round(players.reduce((sum, p) => sum + p.overall, 0) / players.length);
}

const ERA_LABELS: Record<string, string> = {
  CURRENT: "Current",
  CLASSIC: "Classic",
  ALL_TIME: "All-Time",
};

function EraBadge({ eraTag }: { eraTag: string }) {
  return <span className="era-badge">{ERA_LABELS[eraTag] ?? eraTag}</span>;
}

function HiddenCard({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button type="button" className="hidden-card" onClick={onClick}>
      <span className="hidden-card-mark">?</span>
      <span className="hidden-card-hint">{label}</span>
    </button>
  );
}

function StarterSlot({
  player,
  hidden,
  onToggle,
}: {
  player: PlayerSummary;
  hidden: boolean;
  onToggle: () => void;
}) {
  if (hidden) {
    return (
      <div className="starter-slot">
        <span className="starter-slot-position">{player.position ?? "?"}</span>
        <HiddenCard label="Tap to reveal" onClick={onToggle} />
      </div>
    );
  }

  const team = teamStyle(player.team);
  return (
    <button type="button" className="starter-slot starter-slot-clickable" onClick={onToggle}>
      <span className="starter-slot-position">{player.position ?? "?"}</span>
      <PlayerAvatar name={player.name} accentColor={team.color} size={64} />
      <span className="starter-slot-name">{player.name}</span>
      <span className="player-card-meta" style={{ justifyContent: "center" }}>
        <span className="team-chip" style={{ background: team.color }}>
          {team.abbr}
        </span>
        <span className="ovr-badge" style={{ background: ovrColor(player.overall) }}>
          {player.overall}
        </span>
      </span>
      <EraBadge eraTag={player.eraTag} />
    </button>
  );
}

function BenchCard({
  player,
  hidden,
  onToggle,
}: {
  player: PlayerSummary;
  hidden: boolean;
  onToggle: () => void;
}) {
  if (hidden) {
    return (
      <div className="player-card">
        <HiddenCard label="Bench player — tap to reveal" onClick={onToggle} />
      </div>
    );
  }

  const team = teamStyle(player.team);
  return (
    <button type="button" className="player-card player-card-clickable" onClick={onToggle}>
      <PlayerAvatar name={player.name} accentColor={team.color} />
      <div className="player-card-info">
        <div className="player-card-name">{player.name}</div>
        <div className="player-card-meta">
          <span className="team-chip" style={{ background: team.color }}>
            {team.abbr}
          </span>
          <span className="position-badge position-badge-inline">{player.position ?? "?"}</span>
          <span className="ovr-badge" style={{ background: ovrColor(player.overall) }}>
            {player.overall}
          </span>
          <EraBadge eraTag={player.eraTag} />
        </div>
      </div>
    </button>
  );
}

function TeamPanel({ label, roster }: { label: string; roster: GeneratedRoster }) {
  const [hiddenIds, setHiddenIds] = useState<Set<number>>(new Set());

  function toggle(id: number) {
    setHiddenIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function revealAll() {
    setHiddenIds(new Set());
  }

  function revealStarters() {
    const starterIds = new Set(roster.starters.map((p) => p.id));
    setHiddenIds((prev) => new Set([...prev].filter((id) => !starterIds.has(id))));
  }

  function revealBench() {
    const benchIds = new Set(roster.bench.map((p) => p.id));
    setHiddenIds((prev) => new Set([...prev].filter((id) => !benchIds.has(id))));
  }

  return (
    <div className="team-panel">
      <div className="team-panel-header">
        <h2>{label}</h2>
        <span className="team-avg">{average(roster.starters)} OVR avg (starters)</span>
      </div>

      <div className="reveal-controls">
        <button type="button" onClick={revealAll}>
          Reveal all
        </button>
        <button type="button" onClick={revealStarters}>
          Reveal starters
        </button>
        <button type="button" onClick={revealBench}>
          Reveal bench
        </button>
      </div>

      <div className="starters-row">
        {roster.starters.map((p) => (
          <StarterSlot key={p.id} player={p} hidden={hiddenIds.has(p.id)} onToggle={() => toggle(p.id)} />
        ))}
      </div>

      {roster.bench.length > 0 && (
        <>
          <div className="section-label">Bench</div>
          <div className="player-grid">
            {roster.bench.map((p) => (
              <BenchCard key={p.id} player={p} hidden={hiddenIds.has(p.id)} onToggle={() => toggle(p.id)} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

export default function TeamMatchup({ teamA, teamB }: { teamA: GeneratedRoster; teamB: GeneratedRoster }) {
  return (
    <div className="matchup-grid">
      <TeamPanel label="Team A" roster={teamA} />
      <TeamPanel label="Team B" roster={teamB} />
    </div>
  );
}
