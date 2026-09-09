import { NextRequest, NextResponse } from "next/server";
import { analyzeMatchup } from "@/lib/core-client";

export async function POST(request: NextRequest) {
  const body = await request.json();
  try {
    const analysis = await analyzeMatchup(body.teamAPlayerIds ?? [], body.teamBPlayerIds ?? []);
    return NextResponse.json(analysis);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 422 });
  }
}
