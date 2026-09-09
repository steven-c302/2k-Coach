// Server-only. Resolves a display name to an NBA.com headshot URL by name-matching
// against the league's public player list. Nothing here is ever written to disk or
// committed — the index lives in memory for this process and is re-fetched
// periodically, and headshot images are served by redirecting the browser straight
// to NBA's own CDN rather than us fetching/storing the bytes ourselves.

const INDEX_TTL_MS = 12 * 60 * 60 * 1000; // 12h

let cachedIndex: Map<string, number> | null = null;
let cachedAt = 0;
let inFlight: Promise<Map<string, number>> | null = null;

function normalizeName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "") // strip accents
    .replace(/\([^)]*\)/g, " ") // drop "(Legend)"-style edition suffixes
    .toLowerCase()
    .replace(/[^a-z\s]/g, " ")
    .replace(/\b(jr|sr|ii|iii|iv)\b/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

async function fetchNbaPlayerIndex(): Promise<Map<string, number>> {
  const url =
    "https://stats.nba.com/stats/commonallplayers?LeagueID=00&Season=2025-26&IsOnlyCurrentSeason=0";
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 6000);

  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
        Accept: "application/json, text/plain, */*",
        "Accept-Language": "en-US,en;q=0.9",
        Referer: "https://www.nba.com/",
        Origin: "https://www.nba.com",
        "x-nba-stats-origin": "stats",
        "x-nba-stats-token": "true",
      },
    });
    if (!response.ok) return new Map();

    const body = await response.json();
    const resultSet = body?.resultSets?.[0];
    const headers: string[] = resultSet?.headers ?? [];
    const rows: unknown[][] = resultSet?.rowSet ?? [];
    const idCol = headers.indexOf("PERSON_ID");
    const nameCol = headers.indexOf("DISPLAY_FIRST_LAST");
    if (idCol === -1 || nameCol === -1) return new Map();

    const index = new Map<string, number>();
    for (const row of rows) {
      const id = row[idCol];
      const name = row[nameCol];
      if (typeof id === "number" && typeof name === "string") {
        index.set(normalizeName(name), id);
      }
    }
    return index;
  } catch {
    return new Map();
  } finally {
    clearTimeout(timeout);
  }
}

async function getIndex(): Promise<Map<string, number>> {
  const isFresh = cachedIndex && Date.now() - cachedAt < INDEX_TTL_MS;
  if (isFresh) return cachedIndex!;
  if (inFlight) return inFlight;

  inFlight = fetchNbaPlayerIndex().then((index) => {
    if (index.size > 0) {
      cachedIndex = index;
      cachedAt = Date.now();
    }
    inFlight = null;
    return cachedIndex ?? index;
  });
  return inFlight;
}

export async function resolveHeadshotUrl(playerName: string): Promise<string | null> {
  const index = await getIndex();
  const personId = index.get(normalizeName(playerName));
  if (!personId) return null;
  return `https://cdn.nba.com/headshots/nba/latest/1040x760/${personId}.png`;
}
