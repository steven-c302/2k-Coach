export type TeamStyle = {
  abbr: string;
  color: string;
};

const TEAMS: Record<string, TeamStyle> = {
  "Atlanta Hawks": { abbr: "ATL", color: "#e03a3e" },
  "Boston Celtics": { abbr: "BOS", color: "#007a33" },
  "Brooklyn Nets": { abbr: "BKN", color: "#000000" },
  "Charlotte Hornets": { abbr: "CHA", color: "#1d1160" },
  "Chicago Bulls": { abbr: "CHI", color: "#ce1141" },
  "Cleveland Cavaliers": { abbr: "CLE", color: "#860038" },
  "Dallas Mavericks": { abbr: "DAL", color: "#00538c" },
  "Denver Nuggets": { abbr: "DEN", color: "#0e2240" },
  "Detroit Pistons": { abbr: "DET", color: "#c8102e" },
  "Golden State Warriors": { abbr: "GSW", color: "#1d428a" },
  "Houston Rockets": { abbr: "HOU", color: "#ce1141" },
  "Indiana Pacers": { abbr: "IND", color: "#002d62" },
  "LA Clippers": { abbr: "LAC", color: "#c8102e" },
  "Los Angeles Clippers": { abbr: "LAC", color: "#c8102e" },
  "Los Angeles Lakers": { abbr: "LAL", color: "#552583" },
  "Memphis Grizzlies": { abbr: "MEM", color: "#5d76a9" },
  "Miami Heat": { abbr: "MIA", color: "#98002e" },
  "Milwaukee Bucks": { abbr: "MIL", color: "#00471b" },
  "Minnesota Timberwolves": { abbr: "MIN", color: "#0c2340" },
  "New Orleans Pelicans": { abbr: "NOP", color: "#0c2340" },
  "New York Knicks": { abbr: "NYK", color: "#006bb6" },
  "Oklahoma City Thunder": { abbr: "OKC", color: "#007ac1" },
  "Orlando Magic": { abbr: "ORL", color: "#0077c0" },
  "Philadelphia 76ers": { abbr: "PHI", color: "#006bb6" },
  "Phoenix Suns": { abbr: "PHX", color: "#e56020" },
  "Portland Trail Blazers": { abbr: "POR", color: "#e03a3e" },
  "Sacramento Kings": { abbr: "SAC", color: "#5a2d81" },
  "San Antonio Spurs": { abbr: "SAS", color: "#c4ced4" },
  "Toronto Raptors": { abbr: "TOR", color: "#ce1141" },
  "Utah Jazz": { abbr: "UTA", color: "#002b5c" },
  "Washington Wizards": { abbr: "WAS", color: "#002b5c" },
  // Historical/relocated franchises that show up in classic eras.
  "Seattle SuperSonics": { abbr: "SEA", color: "#007ac1" },
  "New Jersey Nets": { abbr: "NJN", color: "#000000" },
  "Vancouver Grizzlies": { abbr: "VAN", color: "#5d76a9" },
  "Charlotte Bobcats": { abbr: "CHA", color: "#1d1160" },
  "New Orleans Hornets": { abbr: "NOH", color: "#1d1160" },
  "New Orleans/Oklahoma City Hornets": { abbr: "NOK", color: "#1d1160" },
  "Washington Bullets": { abbr: "WSB", color: "#002b5c" },
};

const FALLBACK: TeamStyle = { abbr: "FA", color: "#5b6472" };

export function teamStyle(teamName: string | null | undefined): TeamStyle {
  if (!teamName) return FALLBACK;
  return TEAMS[teamName] ?? { ...FALLBACK, abbr: abbreviateUnknown(teamName) };
}

function abbreviateUnknown(teamName: string): string {
  const words = teamName.split(/\s+/).filter(Boolean);
  if (words.length === 1) return words[0].slice(0, 3).toUpperCase();
  return words.map((w) => w[0]).join("").slice(0, 3).toUpperCase();
}
