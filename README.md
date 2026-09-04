# NBA2K Assistant

A tool for building NBA 2K rosters, analyzing pregame matchups, and coaching
live head-to-head sessions in real time, built for a friend group's own
2K26/27 sessions.

This repo replaces two disconnected scripts (`nba2k-data-scraper`,
`nba2k-roster-randomizer`) with a real, working system: Java/Spring Boot
backend, a Python computer-vision microservice, a Next.js frontend, and a
dual-database split (Postgres + DynamoDB). See
[`docs/plan.md`](docs/plan.md) for the full design doc this was built from —
every architectural decision below is explained there in more depth (that doc
predates the current name and still says "Court Vision" throughout; the
architecture and reasoning are unchanged, only the name is).

## Architecture

```
web (Next.js)          --REST/WebSocket-->  core (Spring Boot)
                                                   |        |
vision (FastAPI)        --REST events-->----------+        |
                                              Postgres  DynamoDB (LocalStack)
```

- **`core`** is the single source of truth. It owns Postgres and
  DynamoDB, exposes REST + WebSocket/STOMP, runs the coaching rules engine,
  and is the *only* writer to both databases — no dual-write races.
- **`vision`** watches the game window via screen-capture OCR and
  POSTs structured events to core. It has no game-domain logic — it only
  reports what it observed; "what does this mean" reasoning stays in Java.
  This is a language-fit split (OpenCV/EasyOCR are Python-native), not
  microservices for their own sake — everything else stays one deployable.
- **`web`** is the team builder UI, live session view, coaching
  feed, and manual tap-tracker.
- **Postgres** holds structured, queryable, low-write data (players, rosters,
  sessions). **DynamoDB** holds append-heavy, single-partition-key event data
  (matchup history, the async LLM coaching log) — a poor fit for Postgres's
  relational strengths.

## Is it fully built? No — here's exactly what works today

Only **Milestone 1 of 10** is done (see the roadmap below). Concretely:

- ✅ You can run the backend, and it serves real player data with filters.
- ✅ The web app loads and proves it can talk to the backend.
- ❌ There is no team builder UI, no live sessions, no matchup analysis, no
  coaching engine, no LLM narration, and no OCR/vision pipeline yet. Those
  are all still unbuilt (Milestones 2–10).

So today this is a data API plus a placeholder frontend, not something you'd
sit down and use to build a fantasy roster or get live coaching yet.

## How to actually run what exists

```bash
cp .env.example .env   # optionally set NBA2KAPI_API_KEY — see below
docker compose up --build
```

Then:
- Open **http://localhost:3000** in a browser — the web app fetches players
  from core and shows a connectivity status line + a sample list. This is the
  entire current frontend; there's no navigation beyond it yet.
- Or skip the frontend and hit the API directly:
  ```bash
  curl "http://localhost:8080/api/players?position=PG&minOverall=90&era=CURRENT"
  ```
  Query params: `position`, `minOverall`, `maxOverall`, `era` (`CURRENT` or a
  classic-team year like `1996`), `name` (substring match).
- `vision` → http://localhost:8001/health (health check only — no capture
  loop exists yet, see Milestone 10 below)
- Postgres → localhost:5432, LocalStack DynamoDB → localhost:4566 (unused
  until Milestone 7)

On first boot with an empty `players` table, core bootstraps itself
automatically: from nba2kapi if `NBA2KAPI_API_KEY` is set in `.env`,
otherwise from the bundled local-scraper JSON (1082 players — names, teams,
positions, overalls, but no attributes or badges since nba2kapi wasn't used).
To get real attribute/badge data instead of the bare-bones fallback, sign up
for a free nba2kapi key at https://github.com/wkoverfield/nba2kapi, put it in
`.env`, then either restart core or re-trigger sync manually:

```bash
curl -X POST http://localhost:8080/api/admin/sync/nba2kapi   # needs API key
curl -X POST http://localhost:8080/api/admin/sync/seed       # local fallback, re-runnable any time
```

### Running `core` without Docker

```bash
cd core
./gradlew bootRun    # needs a Postgres reachable at localhost:5432, see application.yml
./gradlew test       # unit tests, no DB required
```

## Data source

Player attributes/badges are synced from the hosted
[`nba2kapi`](https://github.com/wkoverfield/nba2kapi) REST API into Postgres
on a schedule (`NbaTwoKApiSyncService` — see the daily cron in
`SyncScheduler`), never called per-request. Without an API key configured,
the app falls back to bootstrapping from the existing local scraper's
`players.json` (name/team/position/overall only, no attributes/badges) so it
runs out of the box — see `JsonSeedLoader`.

## Milestone status

See `docs/plan.md` §7 for the full roadmap. Current state:

- [x] **Milestone 1 — Data foundation.** Postgres schema (Flyway), nba2kapi
      sync job + JSON seed fallback, `GET /api/players` with filters.
- [ ] Milestone 2 — Team builder API (`RosterCriterion` pipeline)
- [ ] Milestone 3 — Next.js team builder UI (only a connectivity smoke-test
      page exists today, `web/app/page.tsx`)
- [ ] Milestone 4 — Matchup rules engine
- [ ] Milestone 5 — Live sessions over WebSocket
- [ ] Milestone 6 — Async/versioned LLM narration
- [ ] Milestone 7 — DynamoDB matchup history
- [ ] Milestone 8 — Full docker-compose (Postgres + core + web wired today;
      DynamoDB/LocalStack wired but unused until Milestone 7; vision service
      builds but has no capture loop yet)
- [ ] Milestone 9 — GitHub Actions CI
- [ ] Milestone 10 — Vision microservice (FastAPI app scaffolded with
      `/health`; capture/OCR loop not implemented — calibration profiles in
      `vision/app/capture/regions.py` are unverified placeholders)

## Known gaps / assumptions to verify

- **nba2kapi field names**: the README only documents the `shooting`
  category's attribute field names in full; `AttributeExtractor` searches
  every nested category for known aliases (`speed`, `strength`,
  `interiorDefense`, etc.) and degrades to `null` on a miss rather than
  crashing the sync. Verify field names once a live API key is available.
- **`web`** has no `package-lock.json` yet (no Node available in
  the environment this was scaffolded in) — run `npm install` once locally
  and commit the lockfile, then switch `Dockerfile` from `npm install` to
  `npm ci`.
- **`vision`**'s HUD calibration coordinates are placeholders —
  they need to be measured against a real "Play Now" capture at your actual
  play resolution before OCR produces anything meaningful.
