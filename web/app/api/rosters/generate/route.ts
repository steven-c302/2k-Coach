import { NextRequest, NextResponse } from "next/server";
import { generateRoster } from "@/lib/core-client";

export async function POST(request: NextRequest) {
  const body = await request.json();
  try {
    const roster = await generateRoster(body);
    return NextResponse.json(roster);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 422 });
  }
}
