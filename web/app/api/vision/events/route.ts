import { NextRequest, NextResponse } from "next/server";

const CORE_BASE_URL = process.env.CORE_BASE_URL ?? "http://localhost:8080";

export async function POST(request: NextRequest) {
  const body = await request.json();
  const response = await fetch(`${CORE_BASE_URL}/api/vision/events`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    return NextResponse.json({ error: `core returned ${response.status}` }, { status: 502 });
  }
  return new NextResponse(null, { status: 202 });
}
