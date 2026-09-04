const CORE_BASE_URL = process.env.CORE_BASE_URL ?? "http://localhost:8080";

export type PlayerAttributesSummary = {
  threePt: number | null;
  midRange: number | null;
  layup: number | null;
  dunk: number | null;
  speed: number | null;
  strength: number | null;
  postDefense: number | null;
  perimeterDefense: number | null;
};

export type PlayerSummary = {
  id: number;
  name: string;
  team: string | null;
  position: string | null;
  positions: string[];
  overall: number;
  eraTag: string;
  source: string;
  attributes: PlayerAttributesSummary | null;
};

export type RosterCriterionSpec =
  | { type: "OVERALL_RANGE"; min: number; max: number }
  | { type: "POSITION"; allowed: string[] }
  | { type: "BUILD_AROUND"; playerId: number };

export type RosterGenerateRequest = {
  criteria: RosterCriterionSpec[];
  teamSize: number;
  era: string;
};

export type GeneratedRoster = {
  starters: PlayerSummary[];
  bench: PlayerSummary[];
};

export async function fetchPlayers(params: Record<string, string> = {}): Promise<PlayerSummary[]> {
  const query = new URLSearchParams(params).toString();
  const response = await fetch(`${CORE_BASE_URL}/api/players${query ? `?${query}` : ""}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error(`core returned ${response.status}`);
  }
  return response.json();
}

export async function generateRoster(request: RosterGenerateRequest): Promise<GeneratedRoster> {
  const response = await fetch(`${CORE_BASE_URL}/api/rosters/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
    cache: "no-store",
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.error ?? `core returned ${response.status}`);
  }
  return response.json();
}
