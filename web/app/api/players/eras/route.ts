import { NextResponse } from "next/server";
import { fetchEras } from "@/lib/core-client";

export async function GET() {
  try {
    const eras = await fetchEras();
    return NextResponse.json(eras);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 502 });
  }
}
