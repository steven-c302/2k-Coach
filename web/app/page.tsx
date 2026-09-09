import Link from "next/link";
import RosterBuilderForm from "@/components/RosterBuilderForm";
import PlayerSearch from "@/components/PlayerSearch";

export default function HomePage() {
  return (
    <main>
      <h1>NBA2K Assistant</h1>
      <p style={{ color: "var(--text-muted)" }}>
        Build two teams from real player data, or{" "}
        <Link href="/session">start a live session</Link>. Matchup analysis and coaching feed
        land in later milestones — see the project README.
      </p>
      <RosterBuilderForm />

      <h2 style={{ marginTop: "3rem" }}>Player Search</h2>
      <p style={{ color: "var(--text-muted)", marginTop: "-0.5rem" }}>
        Look up players by era, team, position, and attribute thresholds — e.g. guards with 3PT
        &ge; 85 on the 2016 Golden State Warriors.
      </p>
      <PlayerSearch />
    </main>
  );
}
