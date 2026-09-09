"use client";

import { useState } from "react";

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase();
}

export default function PlayerAvatar({
  name,
  accentColor,
  size = 48,
}: {
  name: string;
  accentColor: string;
  size?: number;
}) {
  const [failed, setFailed] = useState(false);

  if (failed) {
    return (
      <div
        className="player-avatar player-avatar-fallback"
        style={{ width: size, height: size, background: accentColor }}
      >
        {initials(name)}
      </div>
    );
  }

  return (
    <img
      className="player-avatar"
      style={{ width: size, height: size }}
      src={`/api/headshots?name=${encodeURIComponent(name)}`}
      alt={name}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  );
}
