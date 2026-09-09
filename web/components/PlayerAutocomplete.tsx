"use client";

import { useEffect, useRef, useState } from "react";
import type { PlayerSummary } from "@/lib/core-client";

type Props = {
  era: string;
  selected: PlayerSummary | null;
  onSelect: (player: PlayerSummary | null) => void;
};

export default function PlayerAutocomplete({ era, selected, onSelect }: Props) {
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
      if (era && era !== "ALL") params.set("era", era);
      const response = await fetch(`/api/players?${params.toString()}`);
      if (response.ok) {
        setResults(await response.json());
      }
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, era]);

  if (selected) {
    return (
      <div className="autocomplete-selected">
        <span>
          {selected.name} — {selected.team ?? "?"} · {selected.position ?? "?"} · {selected.overall} OVR
        </span>
        <button type="button" onClick={() => onSelect(null)}>
          Clear
        </button>
      </div>
    );
  }

  return (
    <div className="autocomplete">
      <input
        type="text"
        placeholder="Search a player by name..."
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
            <li
              key={p.id}
              onMouseDown={() => {
                onSelect(p);
                setQuery("");
                setResults([]);
              }}
            >
              {p.name} — {p.team ?? "?"} · {p.position ?? "?"} · {p.overall} OVR
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
