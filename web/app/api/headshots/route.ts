import { NextRequest, NextResponse } from "next/server";
import { resolveHeadshotUrl } from "@/lib/headshots";

export async function GET(request: NextRequest) {
  const name = request.nextUrl.searchParams.get("name");
  if (!name) {
    return NextResponse.json({ error: "name is required" }, { status: 400 });
  }

  const url = await resolveHeadshotUrl(name);
  if (!url) {
    return NextResponse.json({ error: "no headshot match" }, { status: 404 });
  }

  return NextResponse.redirect(url);
}
