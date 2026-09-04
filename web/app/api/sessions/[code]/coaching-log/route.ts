import { NextRequest, NextResponse } from "next/server";

const CORE_BASE_URL = process.env.CORE_BASE_URL ?? "http://localhost:8080";

export async function GET(request: NextRequest, { params }: { params: { code: string } }) {
  const response = await fetch(`${CORE_BASE_URL}/api/sessions/${params.code}/coaching-log`, {
    cache: "no-store",
  });
  if (!response.ok) {
    return NextResponse.json({ error: `core returned ${response.status}` }, { status: 502 });
  }
  return NextResponse.json(await response.json());
}
