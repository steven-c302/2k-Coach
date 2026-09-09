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

// A plain `value={n}` on a number input snaps back to "0" the instant the field is cleared while
// typing, and that stray "0" then gets a new digit typed in front of or behind it (e.g. clearing
// then typing "90" comes out "090"). Displaying 0 as an empty field sidesteps that - an empty
// field commits as 0 anyway, which is exactly "leave it blank and it defaults to 0."
function NumberField({
  value,
  onChange,
  min,
  max,
}: {
  value: number;
  onChange: (next: number) => void;
  min?: number;
  max?: number;
}) {
  return (
    <input
      type="number"
      min={min}
      max={max}
      value={value === 0 ? "" : value}
      onChange={(e) => onChange(e.target.value === "" ? 0 : Number(e.target.value))}
    />
  );
}

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
  useAverageTarget: boolean;
  targetAverage: number;
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
  useAverageTarget: false,
  targetAverage: 80,
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
  if (settings.useAverageTarget) {
    criteria.push({ type: "OVERALL_AVERAGE", target: settings.targetAverage });
  }
  return criteria;
}

function TeamSettingsPanel({
  label,
  era,
  settings,
  onChange,
  buildAroundPlayer,
  onBuildAroundChange,
}: {
  label: string;
  era: string;
  settings: TeamGenSettings;
  onChange: (next: TeamGenSettings) => void;
  buildAroundPlayer: PlayerSummary | null;
  onBuildAroundChange: (player: PlayerSummary | null) => void;
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
            <NumberField min={0} max={99} value={settings.minOverall} onChange={(v) => set("minOverall", v)} />
          </label>
          <label>
            Max
            <NumberField min={0} max={99} value={settings.maxOverall} onChange={(v) => set("maxOverall", v)} />
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
            <NumberField min={0} max={15} value={settings.aboveCount} onChange={(v) => set("aboveCount", v)} />
          </label>
          <label>
            players &ge; OVR
            <NumberField min={0} max={99} value={settings.aboveThreshold} onChange={(v) => set("aboveThreshold", v)} />
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
            <NumberField min={0} max={15} value={settings.belowCount} onChange={(v) => set("belowCount", v)} />
          </label>
          <label>
            players &le; OVR
            <NumberField min={0} max={99} value={settings.belowThreshold} onChange={(v) => set("belowThreshold", v)} />
          </label>
        </div>
      )}

      <label className="checkbox-label" style={{ marginTop: "0.6rem" }}>
        <input
          type="checkbox"
          checked={settings.useAverageTarget}
          onChange={(e) => set("useAverageTarget", e.target.checked)}
        />
        Target a team average overall
      </label>
      {settings.useAverageTarget && (
        <div className="field-row">
          <label>
            Average OVR
            <NumberField min={0} max={99} value={settings.targetAverage} onChange={(v) => set("targetAverage", v)} />
          </label>
        </div>
      )}
      {(settings.useAboveQuota || settings.useBelowQuota || settings.useAverageTarget) && (
        <p className="field-hint">
          A quota or average target can pick players outside the Overall range above when needed
          to hit it — e.g. two 60s and a 90 to average 70, even with a narrower range set.
        </p>
      )}

      <div style={{ marginTop: "0.6rem" }}>
        <span style={{ fontSize: "0.85rem", fontWeight: 600, display: "block", marginBottom: "0.3rem" }}>
          Build around a player (optional)
        </span>
        <PlayerAutocomplete era={era} selected={buildAroundPlayer} onSelect={onBuildAroundChange} />
      </div>
    </fieldset>
  );
}

export default function RosterBuilderForm() {
  const [teamSize, setTeamSize] = useState(10);
  const [era, setEra] = useState("CURRENT");

  const [usePositionFilter, setUsePositionFilter] = useState(false);
  const [allowedPositions, setAllowedPositions] = useState<string[]>([...POSITIONS]);

  const [teamASettings, setTeamASettings] = useState<TeamGenSettings>(DEFAULT_TEAM_SETTINGS);
  const [teamBSettings, setTeamBSettings] = useState<TeamGenSettings>(DEFAULT_TEAM_SETTINGS);

  const [buildAroundA, setBuildAroundA] = useState<PlayerSummary | null>(null);
  const [buildAroundB, setBuildAroundB] = useState<PlayerSummary | null>(null);

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
    if (buildAroundA) {
      criteriaA.push({ type: "BUILD_AROUND", playerId: buildAroundA.id });
    }

    try {
      const generatedA = await requestRoster(criteriaA, teamSize, era);
      const usedIds = [...generatedA.starters, ...generatedA.bench].map((p) => p.id);

      const criteriaB: RosterCriterionSpec[] = [
        ...settingsToCriteria(teamBSettings),
        ...positionCriteria,
        { type: "EXCLUDE_IDS", ids: usedIds },
      ];
      // If Team B's own build-around pick happens to already be on Team A (possible when both
      // teams draw from the same pool), it can't be excluded and anchored at once - drop the
      // exclude for that one id so the anchor resolution in RosterCriterionFactory still finds it.
      if (buildAroundB) {
        criteriaB.push({ type: "BUILD_AROUND", playerId: buildAroundB.id });
        const withoutAnchor = criteriaB.find(
          (c): c is Extract<RosterCriterionSpec, { type: "EXCLUDE_IDS" }> => c.type === "EXCLUDE_IDS"
        );
        if (withoutAnchor) {
          withoutAnchor.ids = withoutAnchor.ids.filter((id) => id !== buildAroundB.id);
        }
      }

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
            <NumberField min={5} max={15} value={teamSize} onChange={setTeamSize} />
          </label>
          <label>
            Era
            <select value={era} onChange={(e) => setEra(e.target.value)}>
              <option value="CURRENT">Current rosters</option>
              <option value="CLASSIC">Classic (any era/team)</option>
              <option value="ALL_TIME">All-Time teams</option>
              <option value="ALL">All eras</option>
            </select>
          </label>
        </div>

        <div className="matchup-grid" style={{ marginTop: 0 }}>
          <TeamSettingsPanel
            label="Team A settings"
            era={era}
            settings={teamASettings}
            onChange={setTeamASettings}
            buildAroundPlayer={buildAroundA}
            onBuildAroundChange={setBuildAroundA}
          />
          <TeamSettingsPanel
            label="Team B settings"
            era={era}
            settings={teamBSettings}
            onChange={setTeamBSettings}
            buildAroundPlayer={buildAroundB}
            onBuildAroundChange={setBuildAroundB}
          />
        </div>

        <fieldset>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={usePositionFilter}
              onChange={(e) => setUsePositionFilter(e.target.checked)}
            />
            Only draft from specific positions (both teams)
          </label>
          <p className="field-hint">
            Bench spots are always automatically spread across all 5 positions on their own — this
            is a separate, optional restriction on which positions are eligible to be picked at
            all (e.g. only PG and C). Leave unchecked for normal, fully balanced teams.
          </p>
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

        <button type="submit" disabled={loading}>
          {loading ? "Generating..." : "Generate teams"}
        </button>
      </form>

      {error && <p className="status-pill error">{error}</p>}
      {teamA && teamB && <TeamMatchup teamA={teamA} teamB={teamB} />}
    </div>
  );
}
