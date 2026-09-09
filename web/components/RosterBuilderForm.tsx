"use client";

import { useState } from "react";
import type { GeneratedRoster, PlayerSummary, RosterCriterionSpec } from "@/lib/core-client";
import PlayerAutocomplete from "./PlayerAutocomplete";
import TeamMatchup from "./TeamMatchup";

async function requestRoster(criteria: RosterCriterionSpec[], teamSize: number, era: string): Promise<GeneratedRoster> {
  const response = await fetch("/api/rosters/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ criteria, teamSize, era }),
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.error ?? "Failed to generate roster");
  }
  return body as GeneratedRoster;
}

const POSITIONS = ["PG", "SG", "SF", "PF", "C"];

export default function RosterBuilderForm() {
  const [teamSize, setTeamSize] = useState(8);
  const [era, setEra] = useState("CURRENT");

  const [useOverallRange, setUseOverallRange] = useState(true);
  const [minOverall, setMinOverall] = useState(80);
  const [maxOverall, setMaxOverall] = useState(99);

  const [usePositionFilter, setUsePositionFilter] = useState(false);
  const [allowedPositions, setAllowedPositions] = useState<string[]>([...POSITIONS]);

  const [buildAroundPlayer, setBuildAroundPlayer] = useState<PlayerSummary | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [teamA, setTeamA] = useState<GeneratedRoster | null>(null);
  const [teamB, setTeamB] = useState<GeneratedRoster | null>(null);

  function togglePosition(position: string) {
    setAllowedPositions((prev) =>
      prev.includes(position) ? prev.filter((p) => p !== position) : [...prev, position]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setTeamA(null);
    setTeamB(null);

    const sharedCriteria: RosterCriterionSpec[] = [];
    if (useOverallRange) {
      sharedCriteria.push({ type: "OVERALL_RANGE", min: minOverall, max: maxOverall });
    }
    if (usePositionFilter && allowedPositions.length > 0) {
      sharedCriteria.push({ type: "POSITION", allowed: allowedPositions });
    }
    // Build-around only makes sense for one team; Team B never anchors on the same player.
    const criteriaA = buildAroundPlayer
      ? [...sharedCriteria, { type: "BUILD_AROUND" as const, playerId: buildAroundPlayer.id }]
      : sharedCriteria;

    try {
      const generatedA = await requestRoster(criteriaA, teamSize, era);
      const usedIds = [...generatedA.starters, ...generatedA.bench].map((p) => p.id);
      const generatedB = await requestRoster(
        [...sharedCriteria, { type: "EXCLUDE_IDS", ids: usedIds }],
        teamSize,
        era
      );
      setTeamA(generatedA);
      setTeamB(generatedB);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="builder">
      <form onSubmit={handleSubmit} className="builder-form">
        <div className="field-row">
          <label>
            Team size
            <input
              type="number"
              min={5}
              max={15}
              value={teamSize}
              onChange={(e) => setTeamSize(Number(e.target.value))}
            />
          </label>
          <label>
            Era
            <select value={era} onChange={(e) => setEra(e.target.value)}>
              <option value="CURRENT">Current rosters</option>
              <option value="1996">1996 (classic)</option>
              <option value="2013">2013 (classic)</option>
              <option value="2016">2016 (classic)</option>
              <option value="2019">2019 (classic)</option>
            </select>
          </label>
        </div>

        <fieldset>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={useOverallRange}
              onChange={(e) => setUseOverallRange(e.target.checked)}
            />
            Overall range
          </label>
          {useOverallRange && (
            <div className="field-row">
              <label>
                Min
                <input
                  type="number"
                  min={0}
                  max={99}
                  value={minOverall}
                  onChange={(e) => setMinOverall(Number(e.target.value))}
                />
              </label>
              <label>
                Max
                <input
                  type="number"
                  min={0}
                  max={99}
                  value={maxOverall}
                  onChange={(e) => setMaxOverall(Number(e.target.value))}
                />
              </label>
            </div>
          )}
        </fieldset>

        <fieldset>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={usePositionFilter}
              onChange={(e) => setUsePositionFilter(e.target.checked)}
            />
            Limit positions
          </label>
          {usePositionFilter && (
            <div className="position-toggles">
              {POSITIONS.map((position) => (
                <label key={position} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={allowedPositions.includes(position)}
                    onChange={() => togglePosition(position)}
                  />
                  {position}
                </label>
              ))}
            </div>
          )}
        </fieldset>

        <fieldset>
          <legend>Build around a player (optional)</legend>
          <PlayerAutocomplete era={era} selected={buildAroundPlayer} onSelect={setBuildAroundPlayer} />
        </fieldset>

        <button type="submit" disabled={loading}>
          {loading ? "Generating..." : "Generate teams"}
        </button>
      </form>

      {error && <p className="status-pill error">{error}</p>}
      {teamA && teamB && <TeamMatchup teamA={teamA} teamB={teamB} />}
    </div>
  );
}
