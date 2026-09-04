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

**Milestones 1–9 of 10** are done (see the roadmap below). Concretely:

- ✅ Real player data with filters, via API and in the browser.
- ✅ A team builder UI at `/` — filters, team size, era, and a "build around
  this player" autocomplete, generating a real roster.
- ✅ Matchup analysis via API — deterministic rules, no LLM.
- ✅ Live sessions at `/session` — host/join by code, ready-up, broadcast over
  WebSocket. Two browser tabs joining the same code see each other update in
  real time.
- ✅ From inside a live session, trigger a matchup analysis and get a
  narrated coaching tip broadcast to everyone in the session — async, with
  version-based staleness discarding if a newer request supersedes an
  in-flight one, logged to DynamoDB either way (`CoachingEventLog`). Falls
  back to a free local template narrator when no `ANTHROPIC_API_KEY` is set
  — see "Data source" below.
- ✅ Every matchup analysis (session-triggered or bare API call) is appended
  to a DynamoDB history table (`MatchupHistory`), queryable per session.
- ✅ The whole stack runs from one `docker compose up --build` with real
  health-check-gated startup ordering, and CI (`.github/workflows/ci.yml`)
  runs JUnit + Testcontainers, a web typecheck/build, and vision's pytest
  suite on every push/PR.
- ❌ Rosters generated in the team builder aren't wired into a session yet —
  you type player ids by hand into the analyze form. No coaching *rules*
  UI (you see the narration, not the raw mismatch list, in the session view).
  No OCR/vision pipeline. That's Milestone 10, deliberately last per the
  plan — the hardest, most differentiating piece, and it needs a real "Play
  Now" capture on your PC to calibrate, not something buildable/verifiable
  end-to-end in this environment.

So today you can build a roster, run a live join/ready session with a
friend, and from inside that session trigger a real (or template-narrated)
coaching tip with full async/staleness handling and an auditable DynamoDB
trail — genuinely the most resume-relevant piece of the whole plan, and it's
demoable end to end, not just unit-tested.

## How to actually run what exists

```bash
cp .env.example .env   # optionally set NBA2KAPI_API_KEY — see below
docker compose up --build
```

Then:
- Open **http://localhost:3000** — the team builder: set filters, optionally
  search a player to build around, hit "Generate roster."
- Open **http://localhost:3000/session** to host or join a live session —
  open it in two tabs (or two browsers) with the same code to see ready
  state and status sync live between them.
- Or skip the frontend and hit the API directly:
  ```bash
  curl "http://localhost:8080/api/players?position=PG&minOverall=90&era=CURRENT"
  ```
  Query params: `position`, `minOverall`, `maxOverall`, `era` (`CURRENT` or a
  classic-team year like `1996`), `name` (substring match).
- Generate a roster:
  ```bash
  curl -X POST http://localhost:8080/api/rosters/generate \
    -H "Content-Type: application/json" \
    -d '{
      "criteria": [
        {"type": "OVERALL_RANGE", "min": 85, "max": 99},
        {"type": "POSITION", "allowed": ["PG","SG","SF","PF","C"]},
        {"type": "BUILD_AROUND", "playerId": 43}
      ],
      "teamSize": 8,
      "era": "CURRENT"
    }'
  ```
  `criteria` is an ordered pipeline (each filters/reorders the output of the
  one before it) — `OVERALL_RANGE`, `POSITION`, and `BUILD_AROUND` are all
  optional and composable. `BUILD_AROUND` pins that player as their
  position's starter and ranks the rest of the pool by how well they
  complement the anchor's attributes (only meaningful once real attribute
  data is synced from nba2kapi — see below). Without `BUILD_AROUND`, the
  pool is shuffled before filling, matching the old randomizer tool's
  behavior. Returns `422` with an error message if the filtered pool can't
  fill the roster (e.g. no players left at a required position).
- Analyze a matchup (two lists of 5 player ids each):
  ```bash
  curl -X POST http://localhost:8080/api/matchups/analyze \
    -H "Content-Type: application/json" \
    -d '{"teamAPlayerIds": [5,6,13,21,43], "teamBPlayerIds": [222,232,233,236,242]}'
  ```
  Runs 4 deterministic rules (`ThreePointVolumeRule`, `PostSizeRule`,
  `SpeedMismatchRule`, `BadgeExploitRule`) and returns any mismatches found —
  category, which team it favors, severity, and a plain-English evidence
  string. **In practice this returns `{"mismatches": []}` against the seeded
  demo data**, because the local-scraper fallback has no attributes or badges
  to compare — the rules correctly stay silent rather than fabricate a
  result from missing data. Sync real attribute data from nba2kapi (below) to
  see it actually fire, or see the "Known gaps" section for how this was
  verified.
- Live sessions:
  ```bash
  curl -X POST http://localhost:8080/api/sessions   # {"code": "K3F9QZ"}
  ```
  Then connect over STOMP to `ws://localhost:8080/ws`, subscribe to
  `/topic/sessions/{code}`, and send a join frame to
  `/app/sessions/{code}/join` with `{"clientId": "...", "role": "HOST"|"GUEST"}`
  — the web app's `/session/{code}` page does exactly this
  (`@stomp/stompjs`), it's the easiest way to see it work.
- From inside a session (the web page), fill in comma-separated player ids
  for two teams and click "Analyze matchup" — this sends
  `/app/sessions/{code}/analyze`, which bumps the session's version,
  runs the rules engine, and dispatches a narration request on a dedicated
  executor (never the session's own low-latency thread). The narration
  broadcasts to `/topic/sessions/{code}` and appears in every connected tab
  — unless a newer analyze request beat it back, in which case it's silently
  discarded rather than shown out of order. Check the audit trail either way:
  ```bash
  curl http://localhost:8080/api/sessions/{code}/coaching-log     # every request, DELIVERED or DISCARDED_STALE
  curl http://localhost:8080/api/matchups/history/{code}          # every analysis, one row per call
  ```
- `vision` → http://localhost:8001/health (health check only — no capture
  loop exists yet, see Milestone 10 below)
- Postgres → localhost:5432, LocalStack DynamoDB → localhost:4566 (tables
  `CoachingEventLog`/`MatchupHistory` auto-created on first boot — see
  `DynamoDbTableInitializer`)

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

Coaching narration uses the official [`anthropic-java`](https://github.com/anthropics/anthropic-sdk-java)
SDK (`AnthropicLlmClient`, `claude-opus-5` at low effort by default — both
configurable via `ANTHROPIC_MODEL`) when `ANTHROPIC_API_KEY` is set.
**Without a key, `LlmClientSelector` falls back to `TemplateNarrationClient`
— a free, local, deterministic narrator, not a stub** — so the async/staleness
pipeline is fully demoable with zero external cost or dependency. This mirrors
the nba2kapi/local-scraper fallback pattern from Milestone 1 exactly. If you
do add a key, be aware it's usage-billed on your Anthropic account per
narration request — swap `ANTHROPIC_MODEL` to `claude-haiku-4-5` for a much
cheaper option if you want real narration without Opus-tier pricing.

## Milestone status

See `docs/plan.md` §7 for the full roadmap. Current state:

- [x] **Milestone 1 — Data foundation.** Postgres schema (Flyway), nba2kapi
      sync job + JSON seed fallback, `GET /api/players` with filters.
- [x] **Milestone 2 — Team builder API.** `RosterCriterion` pipeline
      (`OverallRangeCriterion`, `PositionCriterion`, `EraCriterion`,
      `BuildAroundPlayerCriterion`), ported starter/bench selection
      (`RosterFillService`), `POST /api/rosters/generate`.
- [x] **Milestone 3 — Next.js team builder UI.** Form + roster display at
      `/` (`RosterBuilderForm`, `PlayerAutocomplete`, `RosterResult`), proxied
      through Next.js route handlers so the browser never talks to core
      directly.
- [x] **Milestone 4 — Matchup rules engine.** `MismatchRule` pipeline
      (`ThreePointVolumeRule`, `PostSizeRule`, `SpeedMismatchRule`,
      `BadgeExploitRule`) over a pure `TeamSnapshot`, `POST
      /api/matchups/analyze`. `SpeedMismatchRule` is adapted from the plan's
      spec (speed vs. speed, not speed vs. an untracked "lateral quickness"
      attribute) — see `SpeedMismatchRule`'s javadoc.
- [x] **Milestone 5 — Live sessions over WebSocket.** Per-session
      single-writer actor (`SessionActor`, virtual-thread executor) behind a
      `ConcurrentHashMap<String, SessionActor>` registry (`SessionRegistry`);
      STOMP endpoints for join/ready (`SessionWebSocketController`),
      broadcasting a new immutable `SessionState` snapshot after each
      processed mutation. `/session` in the web app demos it — verified live
      with two browser tabs sharing state, and with a 500-mutation/50-thread
      concurrency test (`SessionActorConcurrencyTest`) proving no lost
      updates.
- [x] **Milestone 6 — Async/versioned LLM narration.** Implements the plan's
      pseudocode literally: `CoachingNarrationService` runs the rules engine
      synchronously (fast), dispatches the LLM call on a dedicated
      `llmExecutor` (4 bounded threads, never the session actor's pool), and
      on return compares the live session version against the version
      captured when the request started — stale responses are discarded, not
      broadcast. Every request/response, delivered or discarded, is written
      to DynamoDB (`CoachingEventLogEntry`). Verified live: triggered
      `/analyze` from the browser, watched the narration broadcast to the
      session, confirmed the `DELIVERED` row via
      `GET /api/sessions/{code}/coaching-log`. The stale-discard race itself
      is proven by a dedicated test (`CoachingNarrationServiceTest`) using a
      fake, controllable-delay `LlmClient` — exactly what the plan's testing
      strategy calls for, not a real API call.
- [x] **Milestone 7 — DynamoDB matchup history.** Every `MatchupAnalysisService.analyze()`
      call — from the plain REST endpoint or from a session's `/analyze` —
      appends to `MatchupHistory`, keyed by session code (`"STANDALONE"` for
      bare API calls). `GET /api/matchups/history/{sessionCode}` is the
      simple history view. `finalScore` stays `null`: there's no live score
      feed until Milestone 10's OCR pipeline exists, and the field isn't
      faked to look complete. Verified live for both the session-triggered
      and standalone paths.
- [x] **Milestone 8 — Full docker-compose.** All 5 services (Postgres,
      LocalStack, core, vision, web) come up together with one
      `docker compose up --build`. `core` now has a real Docker
      `HEALTHCHECK` (`/actuator/health`), and `vision`/`web` wait on it being
      *healthy*, not just started — verified by bringing up the whole stack
      at once and confirming the dependency ordering actually gates on
      health, then smoke-testing all 5 services.
- [x] **Milestone 9 — GitHub Actions CI.** `.github/workflows/ci.yml`, 3
      jobs: `core-tests` (`./gradlew test`, includes a real Testcontainers
      Postgres integration test — see below), `web-build` (`npm run build`,
      which typechecks), `vision-tests` (`pytest` against a lightweight
      dependency set, skipping the heavy OpenCV/EasyOCR/torch stack that
      `test_health.py`/`test_regions.py` don't touch).
- [ ] Milestone 10 — Vision microservice (FastAPI app scaffolded with
      `/health`; capture/OCR loop not implemented — calibration profiles in
      `vision/app/capture/regions.py` are unverified placeholders)

`core`'s test suite: 69 JUnit tests, `./gradlew test`. Most are pure-logic or
mocked-boundary tests (no DB required), but `PlayerRepositoryIntegrationTest`
is a real Testcontainers-backed Postgres integration test — the plan's
testing strategy calls this out explicitly for query-shape-sensitive logic
("don't mock the DB here"), and it's what CI's `core-tests` job actually
runs. **It fails when run locally in this development sandbox** — Docker's
CLI works here (used throughout for `docker compose`), but Testcontainers
talks to the raw Docker Engine API directly over the named pipe, and that
API is stubbed/restricted in this sandbox regardless of which pipe is
targeted. This is a local environment restriction, not a code bug — the
same test runs for real in CI, where GitHub-hosted runners have native
Docker access; verify a green run at the badge/Actions tab rather than
trusting a local `./gradlew test` for this one test class specifically.

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
- **Multiple entries per player in the local-scraper seed data** (e.g. LeBron
  James: Cavaliers/Heat/Lakers, each a separate row even within the `CURRENT`
  era): this is intentional, not a data quality bug — 2K itself lists a
  player's different team-stint versions as distinct cards, and the roster
  pipeline correctly treats them as distinct players. No fix needed.
- **Matchup rules return nothing against the demo dataset**: `BadgeExploitRule`
  needs badge data and all four rules need attribute data, neither of which
  the local-scraper fallback provides (see Milestone 1). Verified this is
  working-as-designed, not broken, by temporarily inserting real attribute
  rows via `psql` and confirming `ThreePointVolumeRule` fired correctly with
  the right severity and evidence text — then removed the test data. Sync
  from nba2kapi for this to be meaningful against real data.
- **A core restart drops live sessions from memory.** `SessionActor`s live
  only in `SessionRegistry`'s in-process map; the `sessions` Postgres row
  persists, but nothing rehydrates an actor from it on startup. `GET
  /api/sessions/{code}` and any WebSocket join for a session created before
  the last restart will 404 / silently no-op. Rehydration on
  `ApplicationReadyEvent` (mirroring `SyncScheduler`'s bootstrap pattern) is
  the natural fix, not yet built.
