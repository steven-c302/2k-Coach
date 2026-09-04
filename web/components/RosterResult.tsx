import type { GeneratedRoster, PlayerSummary } from "@/lib/core-client";

function PlayerCard({ player }: { player: PlayerSummary }) {
  return (
    <div className="player-card">
      <span className="position-badge">{player.position ?? "?"}</span>
      <div className="player-card-info">
        <div className="player-card-name">{player.name}</div>
        <div className="player-card-meta">
          {player.team ?? "?"} · {player.overall} OVR
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
