import type { GeneratedRoster, PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";
import PlayerCard, { ovrColor } from "./PlayerCard";

function average(players: PlayerSummary[]): number {
  if (players.length === 0) return 0;
  return Math.round(players.reduce((sum, p) => sum + p.overall, 0) / players.length);
}

function StarterSlot({ player }: { player: PlayerSummary }) {
  const team = teamStyle(player.team);
  return (
    <div className="starter-slot">
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
    </div>
  );
}

function TeamPanel({ label, roster }: { label: string; roster: GeneratedRoster }) {
  return (
    <div className="team-panel">
      <div className="team-panel-header">
        <h2>{label}</h2>
        <span className="team-avg">{average(roster.starters)} OVR avg (starters)</span>
      </div>

      <div className="starters-row">
        {roster.starters.map((p) => (
          <StarterSlot key={p.id} player={p} />
        ))}
      </div>

      {roster.bench.length > 0 && (
        <>
          <div className="section-label">Bench</div>
          <div className="player-grid">
            {roster.bench.map((p) => (
              <PlayerCard key={p.id} player={p} />
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
