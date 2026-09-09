import { NextRequest, NextResponse } from "next/server";
import { fetchTeams } from "@/lib/core-client";

export async function GET(request: NextRequest) {
  const era = request.nextUrl.searchParams.get("era");
  if (!era) {
    return NextResponse.json({ error: "era is required" }, { status: 400 });
  }
  try {
    const teams = await fetchTeams(era);
    return NextResponse.json(teams);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 502 });
  }
}
