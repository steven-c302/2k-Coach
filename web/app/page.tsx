import Link from "next/link";
import RosterBuilderForm from "@/components/RosterBuilderForm";

export default function HomePage() {
  return (
    <main>
      <h1>NBA2K Assistant</h1>
      <p style={{ color: "#666" }}>
        Build a roster from real player data, or{" "}
        <Link href="/session">start a live session</Link>. Matchup analysis and coaching feed
        land in later milestones — see the project README.
      </p>
      <RosterBuilderForm />
    </main>
  );
}
