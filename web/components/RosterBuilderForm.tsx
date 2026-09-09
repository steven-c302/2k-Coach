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

type TeamGenSettings = {
  useOverallRange: boolean;
  minOverall: number;
  maxOverall: number;
  useAboveQuota: boolean;
  aboveThreshold: number;
  aboveCount: number;
  useBelowQuota: boolean;
  belowThreshold: number;
  belowCount: number;
};

const DEFAULT_TEAM_SETTINGS: TeamGenSettings = {
  useOverallRange: true,
  minOverall: 80,
  maxOverall: 99,
  useAboveQuota: false,
  aboveThreshold: 90,
  aboveCount: 2,
  useBelowQuota: false,
  belowThreshold: 75,
  belowCount: 2,
};

function settingsToCriteria(settings: TeamGenSettings): RosterCriterionSpec[] {
  const criteria: RosterCriterionSpec[] = [];
  if (settings.useOverallRange) {
    criteria.push({ type: "OVERALL_RANGE", min: settings.minOverall, max: settings.maxOverall });
  }
  if (settings.useAboveQuota || settings.useBelowQuota) {
    criteria.push({
      type: "OVERALL_DISTRIBUTION",
      ...(settings.useAboveQuota
        ? { aboveThreshold: settings.aboveThreshold, aboveCount: settings.aboveCount }
        : {}),
      ...(settings.useBelowQuota
        ? { belowThreshold: settings.belowThreshold, belowCount: settings.belowCount }
        : {}),
    });
  }
  return criteria;
}

function TeamSettingsPanel({
  label,
  settings,
  onChange,
}: {
  label: string;
  settings: TeamGenSettings;
  onChange: (next: TeamGenSettings) => void;
}) {
  function set<K extends keyof TeamGenSettings>(key: K, value: TeamGenSettings[K]) {
    onChange({ ...settings, [key]: value });
  }

  return (
    <fieldset>
      <legend>{label}</legend>

      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={settings.useOverallRange}
          onChange={(e) => set("useOverallRange", e.target.checked)}
        />
        Overall range
      </label>
      {settings.useOverallRange && (
        <div className="field-row">
          <label>
            Min
            <input
              type="number"
              min={0}
              max={99}
              value={settings.minOverall}
              onChange={(e) => set("minOverall", Number(e.target.value))}
            />
          </label>
          <label>
            Max
            <input
              type="number"
              min={0}
              max={99}
              value={settings.maxOverall}
              onChange={(e) => set("maxOverall", Number(e.target.value))}
            />
          </label>
        </div>
      )}

      <label className="checkbox-label" style={{ marginTop: "0.6rem" }}>
        <input
          type="checkbox"
          checked={settings.useAboveQuota}
          onChange={(e) => set("useAboveQuota", e.target.checked)}
        />
        Require players above a rating
      </label>
      {settings.useAboveQuota && (
        <div className="field-row">
          <label>
            At least
            <input
              type="number"
              min={0}
              max={15}
              value={settings.aboveCount}
              onChange={(e) => set("aboveCount", Number(e.target.value))}
            />
          </label>
          <label>
            players &ge; OVR
            <input
              type="number"
              min={0}
              max={99}
              value={settings.aboveThreshold}
              onChange={(e) => set("aboveThreshold", Number(e.target.value))}
            />
          </label>
        </div>
      )}

      <label className="checkbox-label" style={{ marginTop: "0.6rem" }}>
        <input
          type="checkbox"
          checked={settings.useBelowQuota}
          onChange={(e) => set("useBelowQuota", e.target.checked)}
        />
        Require players below a rating
      </label>
      {settings.useBelowQuota && (
        <div className="field-row">
          <label>
            At least
            <input
              type="number"
              min={0}
              max={15}
              value={settings.belowCount}
              onChange={(e) => set("belowCount", Number(e.target.value))}
            />
          </label>
          <label>
            players &le; OVR
            <input
              type="number"
              min={0}
              max={99}
              value={settings.belowThreshold}
              onChange={(e) => set("belowThreshold", Number(e.target.value))}
            />
          </label>
        </div>
      )}
    </fieldset>
  );
}

export default function RosterBuilderForm() {
  const [teamSize, setTeamSize] = useState(8);
  const [era, setEra] = useState("CURRENT");

  const [usePositionFilter, setUsePositionFilter] = useState(false);
  const [allowedPositions, setAllowedPositions] = useState<string[]>([...POSITIONS]);

  const [teamASettings, setTeamASettings] = useState<TeamGenSettings>(DEFAULT_TEAM_SETTINGS);
  const [teamBSettings, setTeamBSettings] = useState<TeamGenSettings>(DEFAULT_TEAM_SETTINGS);

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

    const positionCriteria: RosterCriterionSpec[] =
      usePositionFilter && allowedPositions.length > 0 ? [{ type: "POSITION", allowed: allowedPositions }] : [];

    const criteriaA: RosterCriterionSpec[] = [...settingsToCriteria(teamASettings), ...positionCriteria];
    // Build-around only makes sense for one team; Team B never anchors on the same player.
    if (buildAroundPlayer) {
      criteriaA.push({ type: "BUILD_AROUND", playerId: buildAroundPlayer.id });
    }

    try {
      const generatedA = await requestRoster(criteriaA, teamSize, era);
      const usedIds = [...generatedA.starters, ...generatedA.bench].map((p) => p.id);
      const criteriaB: RosterCriterionSpec[] = [
        ...settingsToCriteria(teamBSettings),
        ...positionCriteria,
        { type: "EXCLUDE_IDS", ids: usedIds },
      ];
      const generatedB = await requestRoster(criteriaB, teamSize, era);
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
              <option value="CLASSIC">Classic (any era/team)</option>
              <option value="ALL_TIME">All-Time teams</option>
            </select>
          </label>
        </div>

        <div className="matchup-grid" style={{ marginTop: 0 }}>
          <TeamSettingsPanel label="Team A settings" settings={teamASettings} onChange={setTeamASettings} />
          <TeamSettingsPanel label="Team B settings" settings={teamBSettings} onChange={setTeamBSettings} />
        </div>

        <fieldset>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={usePositionFilter}
              onChange={(e) => setUsePositionFilter(e.target.checked)}
            />
            Limit positions (both teams)
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
          <legend>Build around a player (optional, Team A only)</legend>
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
