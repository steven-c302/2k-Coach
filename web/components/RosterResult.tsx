import type { GeneratedRoster, PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";

function ovrColor(overall: number): string {
  if (overall >= 90) return "var(--ovr-elite)";
  if (overall >= 85) return "var(--ovr-great)";
  if (overall >= 80) return "var(--ovr-solid)";
  if (overall >= 70) return "var(--ovr-average)";
  return "var(--ovr-low)";
}

function PlayerCard({ player }: { player: PlayerSummary }) {
  const team = teamStyle(player.team);
  return (
    <div className="player-card">
      <PlayerAvatar name={player.name} accentColor={team.color} />
      <div className="player-card-info">
        <div className="player-card-name">{player.name}</div>
        <div className="player-card-meta">
          <span className="team-chip" style={{ background: team.color }}>
            {team.abbr}
          </span>
          <span className="position-badge" style={{ width: "auto", height: "auto", padding: "0.1rem 0.35rem", borderRadius: "5px" }}>
            {player.position ?? "?"}
          </span>
          <span className="ovr-badge" style={{ background: ovrColor(player.overall) }}>
            {player.overall}
          </span>
        </div>
      </div>
    </div>
  );
}

export default function RosterResult({ roster }: { roster: GeneratedRoster }) {
  return (
    <div className="roster-result">
      <section>
        <h2>Starters</h2>
        <div className="player-grid">
          {roster.starters.map((p) => (
            <PlayerCard key={p.id} player={p} />
          ))}
        </div>
      </section>
      {roster.bench.length > 0 && (
        <section>
          <h2>Bench</h2>
          <div className="player-grid">
            {roster.bench.map((p) => (
              <PlayerCard key={p.id} player={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
