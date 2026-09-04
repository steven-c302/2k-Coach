const CORE_BASE_URL = process.env.CORE_BASE_URL ?? "http://localhost:8080";

export type PlayerSummary = {
  id: number;
  name: string;
  team: string | null;
  position: string | null;
  overall: number;
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
