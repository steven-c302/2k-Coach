import type { PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";

export function ovrColor(overall: number): string {
  if (overall >= 90) return "var(--ovr-elite)";
  if (overall >= 85) return "var(--ovr-great)";
  if (overall >= 80) return "var(--ovr-solid)";
  if (overall >= 70) return "var(--ovr-average)";
  return "var(--ovr-low)";
}

export default function PlayerCard({ player }: { player: PlayerSummary }) {
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
          <span className="position-badge position-badge-inline">{player.position ?? "?"}</span>
          <span className="ovr-badge" style={{ background: ovrColor(player.overall) }}>
            {player.overall}
          </span>
        </div>
      </div>
    </div>
  );
}
