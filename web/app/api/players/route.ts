import { NextRequest, NextResponse } from "next/server";
import { fetchPlayers } from "@/lib/core-client";

const FORWARDED_PARAMS = [
  "name",
  "position",
  "minOverall",
  "maxOverall",
  "era",
  "team",
  "minThreePt",
  "minMidRange",
  "minLayup",
  "minDunk",
  "minSpeed",
  "minStrength",
  "minPostDefense",
  "minPerimeterDefense",
];

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const params: Record<string, string> = {};
  for (const key of FORWARDED_PARAMS) {
    const value = searchParams.get(key);
    if (value) params[key] = value;
  }

  try {
    const players = await fetchPlayers(params);
    return NextResponse.json(players);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 502 });
  }
}
