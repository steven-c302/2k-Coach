"use client";

import { useEffect, useRef, useState } from "react";
import type { PlayerSummary } from "@/lib/core-client";
import { teamStyle } from "@/lib/teams";
import PlayerAvatar from "./PlayerAvatar";
import { ovrColor } from "./PlayerCard";

export default function MultiPlayerPicker({
  label,
  selected,
  onChange,
}: {
  label: string;
  selected: PlayerSummary[];
  onChange: (next: PlayerSummary[]) => void;
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<PlayerSummary[]>([]);
  const [open, setOpen] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (query.trim().length < 2) {
      setResults([]);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      const params = new URLSearchParams({ name: query });
      const response = await fetch(`/api/players?${params.toString()}`);
      if (response.ok) {
        setResults(await response.json());
      }
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query]);

  function add(player: PlayerSummary) {
    if (selected.some((p) => p.id === player.id)) return;
    onChange([...selected, player]);
    setQuery("");
    setResults([]);
  }

  function remove(id: number) {
    onChange(selected.filter((p) => p.id !== id));
  }

  return (
    <div>
      <div className="picker-label">{label}</div>

      {selected.length > 0 && (
        <div className="player-grid" style={{ marginBottom: "0.6rem" }}>
          {selected.map((p) => {
            const team = teamStyle(p.team);
            return (
              <button
                type="button"
                key={p.id}
                className="player-card player-card-clickable"
                onClick={() => remove(p.id)}
                title="Remove"
              >
                <PlayerAvatar name={p.name} accentColor={team.color} />
                <div className="player-card-info">
                  <div className="player-card-name">{p.name}</div>
                  <div className="player-card-meta">
                    <span className="team-chip" style={{ background: team.color }}>
                      {team.abbr}
                    </span>
                    <span className="position-badge position-badge-inline">{p.position ?? "?"}</span>
                    <span className="ovr-badge" style={{ background: ovrColor(p.overall) }}>
                      {p.overall}
                    </span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}

      <div className="autocomplete">
        <input
          type="text"
          placeholder="Add a player by name..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
        />
        {open && results.length > 0 && (
          <ul className="autocomplete-results">
            {results.slice(0, 8).map((p) => (
              <li key={p.id} onMouseDown={() => add(p)}>
                {p.name} — {p.team ?? "?"} · {p.position ?? "?"} · {p.overall} OVR
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
