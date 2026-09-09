"use client";

import { useEffect, useMemo, useState } from "react";
import type { PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";
import { ovrColor } from "./PlayerCard";

const POSITIONS = ["PG", "SG", "SF", "PF", "C"];
const ERA_LABELS: Record<string, string> = {
  CURRENT: "Current",
  CLASSIC: "Classic",
  ALL_TIME: "All-Time",
};

const ATTRIBUTES = [
  { key: "minThreePt", label: "3PT" },
  { key: "minMidRange", label: "Mid-range" },
  { key: "minLayup", label: "Layup" },
  { key: "minDunk", label: "Dunk" },
  { key: "minSpeed", label: "Speed" },
  { key: "minStrength", label: "Strength" },
  { key: "minPostDefense", label: "Post D" },
  { key: "minPerimeterDefense", label: "Perimeter D" },
] as const;

type SortKey = "name" | "team" | "position" | "overall";

const RESULT_CAP = 100;

export default function PlayerSearch() {
  const [eras, setEras] = useState<string[]>([]);
  const [era, setEra] = useState("");
  const [teams, setTeams] = useState<string[]>([]);
  const [team, setTeam] = useState("");
  const [position, setPosition] = useState("");
  const [name, setName] = useState("");
  const [attributeMins, setAttributeMins] = useState<Record<string, string>>({});

  const [results, setResults] = useState<PlayerSummary[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sortKey, setSortKey] = useState<SortKey>("overall");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  useEffect(() => {
    fetch("/api/players/eras")
      .then((r) => r.json())
      .then((data) => setEras(Array.isArray(data) ? data : []))
      .catch(() => setEras([]));
  }, []);

  useEffect(() => {
    setTeam("");
    if (!era) {
      setTeams([]);
      return;
    }
    fetch(`/api/players/teams?era=${encodeURIComponent(era)}`)
      .then((r) => r.json())
      .then((data) => setTeams(Array.isArray(data) ? data : []))
      .catch(() => setTeams([]));
  }, [era]);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    if (era) params.set("era", era);
    if (team) params.set("team", team);
    if (position) params.set("position", position);
    if (name.trim()) params.set("name", name.trim());
    for (const attr of ATTRIBUTES) {
      const value = attributeMins[attr.key];
      if (value) params.set(attr.key, value);
    }

    try {
      const response = await fetch(`/api/players?${params.toString()}`);
      const body = await response.json();
      if (!response.ok) throw new Error(body.error ?? "Search failed");
      setResults(body);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
      setResults(null);
    } finally {
      setLoading(false);
    }
  }

  function toggleSort(key: SortKey) {
    if (key === sortKey) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir(key === "overall" ? "desc" : "asc");
    }
  }

  const sorted = useMemo(() => {
    if (!results) return [];
    const copy = [...results];
    copy.sort((a, b) => {
      let cmp = 0;
      if (sortKey === "overall") cmp = a.overall - b.overall;
      else if (sortKey === "name") cmp = a.name.localeCompare(b.name);
      else if (sortKey === "team") cmp = (a.team ?? "").localeCompare(b.team ?? "");
      else if (sortKey === "position") cmp = (a.position ?? "").localeCompare(b.position ?? "");
      return sortDir === "asc" ? cmp : -cmp;
    });
    return copy;
  }, [results, sortKey, sortDir]);

  const shown = sorted.slice(0, RESULT_CAP);

  return (
    <div className="player-search">
      <form onSubmit={handleSearch} className="builder-form">
        <div className="field-row">
          <label>
            Era
            <select value={era} onChange={(e) => setEra(e.target.value)}>
              <option value="">Any era</option>
              {eras.map((e) => (
                <option key={e} value={e}>
                  {ERA_LABELS[e] ?? e}
                </option>
              ))}
            </select>
          </label>
          <label style={{ minWidth: 220 }}>
            Team
            <select value={team} onChange={(e) => setTeam(e.target.value)} disabled={!era}>
              <option value="">{era ? "Any team" : "Pick an era first"}</option>
              {teams.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
          <label>
            Position
            <select value={position} onChange={(e) => setPosition(e.target.value)}>
              <option value="">Any</option>
              {POSITIONS.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </label>
          <label>
            Name
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="Search by name..." />
          </label>
        </div>

        <fieldset>
          <legend>Attribute minimums (optional)</legend>
          <div className="field-row">
            {ATTRIBUTES.map((attr) => (
              <label key={attr.key} style={{ width: 90 }}>
                {attr.label}
                <input
                  type="number"
                  min={0}
                  max={99}
                  placeholder="Any"
                  value={attributeMins[attr.key] ?? ""}
                  onChange={(e) =>
                    setAttributeMins((prev) => ({ ...prev, [attr.key]: e.target.value }))
                  }
                />
              </label>
            ))}
          </div>
        </fieldset>

        <button type="submit" disabled={loading}>
          {loading ? "Searching..." : "Search players"}
        </button>
      </form>

      {error && <p className="status-pill error">{error}</p>}

      {results && (
        <div style={{ marginTop: "1.25rem" }}>
          <div className="section-label">
            {results.length === 0
              ? "No players match those filters"
              : `Showing ${shown.length} of ${results.length} match${results.length === 1 ? "" : "es"}${
                  results.length > RESULT_CAP ? " — refine filters to narrow further" : ""
                }`}
          </div>
          {shown.length > 0 && (
            <div style={{ overflowX: "auto" }}>
              <table>
                <thead>
                  <tr>
                    <th style={{ cursor: "pointer" }} onClick={() => toggleSort("name")}>
                      Player
                    </th>
                    <th style={{ cursor: "pointer" }} onClick={() => toggleSort("team")}>
                      Team
                    </th>
                    <th style={{ cursor: "pointer" }} onClick={() => toggleSort("position")}>
                      Pos
                    </th>
                    <th style={{ cursor: "pointer" }} onClick={() => toggleSort("overall")}>
                      OVR
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {shown.map((p) => {
                    const t = teamStyle(p.team);
                    return (
                      <tr key={p.id}>
                        <td>
                          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                            <PlayerAvatar name={p.name} accentColor={t.color} size={32} />
                            {p.name}
                          </div>
                        </td>
                        <td>{p.team ?? "?"}</td>
                        <td>{p.position ?? "?"}</td>
                        <td>
                          <span className="ovr-badge" style={{ background: ovrColor(p.overall) }}>
                            {p.overall}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
