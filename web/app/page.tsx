import { fetchPlayers } from "@/lib/core-client";

// Connectivity smoke test against core's Milestone 1 endpoint.
// The real team builder UI (form, roster display) is Milestone 3 — deliberately
// not built yet, see the plan's roadmap.
export default async function HomePage() {
  let players: Awaited<ReturnType<typeof fetchPlayers>> = [];
  let error: string | null = null;

  try {
    players = await fetchPlayers({ minOverall: "90" });
  } catch (e) {
    error = e instanceof Error ? e.message : "Unknown error";
  }

  return (
    <main>
      <h1>NBA2K Assistant</h1>
      <p>
        {error ? (
          <span className="status-pill error">core unreachable: {error}</span>
        ) : (
          <span className="status-pill ok">
            core connected — {players.length} players with overall ≥ 90
          </span>
        )}
      </p>
      <p style={{ color: "#666" }}>
        Team builder, live sessions, and coaching feed land in later milestones — see the
        project README.
      </p>
      {players.length > 0 && (
        <ul>
          {players.slice(0, 10).map((p) => (
            <li key={p.id}>
              {p.name} — {p.team ?? "?"} · {p.position ?? "?"} · {p.overall} OVR
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
