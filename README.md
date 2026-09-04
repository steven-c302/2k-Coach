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

## Is it fully built?

**All 10 milestones are done.** Concretely:

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
- ✅ A real capture → preprocess → OCR → parse → emit pipeline in `vision`
  (`mss` capture, OpenCV crop/threshold/upscale, EasyOCR, regex parsing into
  structured events), controllable per-session via `/api/capture/start`
  and `/api/capture/stop`. Core's `POST /api/vision/events` routes an
  incoming event into the matching session and broadcasts the update live.
- ✅ A manual tap-tracker in the session view — the same event-shaped
  endpoint the OCR pipeline uses, so a friend can track the score by hand
  and it flows through the identical path. Verified live end to end: tapped
  a score, watched it broadcast to a second browser tab.
- ❌ Rosters generated in the team builder aren't wired into a session yet —
  you type player ids by hand into the analyze form. No coaching *rules*
  UI (you see the narration, not the raw mismatch list, in the session
  view). The vision pipeline's actual OCR accuracy against a real "Play Now"
  capture is **unverified** — this environment has no NBA 2K game and no
  attached display to test screen capture against; see "Vision pipeline"
  below for exactly what is and isn't proven.

So today you can build a roster, run a live join/ready session with a
friend, trigger a real (or template-narrated) coaching tip with full
async/staleness handling and an auditable DynamoDB trail, and either tap
out the score by hand or (once calibrated on your PC) let OCR read it off
the screen automatically — genuinely the whole system the resume describes,
not a subset of it.

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
- The manual tap-tracker: on any session page, tap "+2"/"+3" for either
  team — it posts through `/api/vision/events` (proxied) into the session's
  `liveGameState`, broadcasting live, same as OCR would.
- `vision` OCR capture (needs a real display with NBA 2K running — see
  "Vision pipeline" below):
  ```bash
  curl -X POST http://localhost:8001/api/capture/start \
    -H "Content-Type: application/json" \
    -d '{"session_id": "K3F9QZ", "resolution": [1920, 1080]}'
  curl http://localhost:8001/api/capture/status
  curl -X POST http://localhost:8001/api/capture/stop -H "Content-Type: application/json" -d '{"session_id": "K3F9QZ"}'
  ```
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

## Vision pipeline: what's verified vs. not

Real code exists for every stage (capture, preprocess, OCR, parse, emit,
control loop) — none of it is a stub. What's actually been proven to work,
and what fundamentally can't be in this environment:

- ✅ **Parsing** (`parse_score`/`parse_clock`/`parse_shot_clock`): pure
  functions, fully unit-tested, including a real OCR-observed failure mode
  (`parse_clock` accepts `.`/`,` as well as `:` because EasyOCR read a
  rendered `"5:23"` back as `"5.23"` — caught by the golden-file test below,
  then fixed and covered by a regression test).
- ✅ **Preprocessing** (`preprocess_region`): unit-tested for correct
  crop/grayscale/upscale math against synthetic arrays.
- ✅ **OCR wrapper + end-to-end text extraction**: `test_ocr_golden.py` runs
  real EasyOCR (not mocked) against a synthetic image rendered with
  `cv2.putText` — proving the OCR→parsing composition genuinely works, not
  just that the functions type-check. **This is not a real NBA 2K
  screenshot** — no game and no real capture fixture exist in this
  environment — so it says nothing about accuracy against the actual
  stylized 2K HUD font.
- ✅ **Event emission**: `EventClient.emit` tested against `httpx.MockTransport`
  — request shape, URL, and error handling all verified without a real
  network call.
- ✅ **Core-side routing**: `POST /api/vision/events` → `SessionActor` →
  broadcast, verified live via curl and via the browser tap-tracker
  end-to-end (two tabs, one taps, both see the update).
- ❌ **Actual screen capture** (`backend.grab_frame`, via `mss`): cannot be
  exercised here at all — this environment has no attached display, and
  `vision`'s own Docker container is headless by design (no X server). This
  is the fundamental reason the plan puts this milestone last and ties its
  dev-environment notes to your actual gaming PC, not a container.
- ❌ **HUD calibration** (`app/capture/regions.py`): the one profile in
  there is an explicit, labeled placeholder guess for 1080p. It has never
  been checked against a real captured frame and almost certainly needs
  real coordinates measured at your PC before OCR reads anything meaningful
  off the actual game.

**To actually finish calibrating this on your PC**: run `vision` natively
(not in Docker, so `mss` can see your real display), take a screenshot
while in a "Play Now" game, measure the pixel coordinates of the score/clock
HUD elements at your resolution, and replace `PLACEHOLDER_1080P` in
`regions.py` with real numbers. The rest of the pipeline (preprocess → OCR →
parse → emit → broadcast) is already working code at that point, not
something else to build.

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
- [x] **Milestone 10 — Vision microservice.** Real pipeline: `mss` capture
      (`app/capture/backend.py`) → OpenCV crop/grayscale/threshold/upscale
      (`app/capture/preprocess.py`) → EasyOCR (`app/ocr/reader.py`) → regex
      parsing (`app/parsing.py`) → `POST /api/vision/events`
      (`app/events.py`, from Milestone 1). `CaptureLoop`
      (`app/capture/loop.py`) ticks on an interval, controllable per-session
      via `/api/capture/start`/`/stop`; a tick failure is logged and skipped,
      never kills the loop. Core's `VisionEventController`/`VisionEventService`
      route an incoming event into the matching session's `SessionActor` and
      broadcast the merged `liveGameState` live. The manual tap-tracker
      (`web/components/TapTracker.tsx`) posts to the identical endpoint.
      **What's genuinely verified vs. not** — see "Vision pipeline" above.

`core`'s test suite: **77 JUnit tests, verified green on real CI**. Locally,
`./gradlew test` shows 75 entries — 74 genuinely pass, and the 3
Testcontainers-backed methods collapse into a single failing
`initializationError` placeholder (JUnit reports one entry when a test
class's setup fails entirely, not one per method) — see the Testcontainers
note below for why. `vision` has 18 pytest tests (16 run in CI's lightweight job; the 2
golden-file OCR tests need the full ML stack, verified via the vision Docker
image). Most core tests are pure-logic or mocked-boundary (no DB required),
but `PlayerRepositoryIntegrationTest` is a real Testcontainers-backed
Postgres integration test — the plan's testing strategy calls this out
explicitly for query-shape-sensitive logic ("don't mock the DB here"), and
it's what CI's `core-tests` job actually runs.

**It fails when run locally in this development sandbox** — Docker's CLI
works here (used throughout for `docker compose`), but Testcontainers talks
to the raw Docker Engine API directly over the named pipe, and that API is
stubbed/restricted in this sandbox regardless of which pipe is targeted.
This is a local environment restriction, not a code bug: the same test runs
for real in CI, where GitHub-hosted runners have native Docker access —
verify a green run at the Actions tab rather than trusting a local
`./gradlew test` for this one test class specifically. Two real bugs
surfaced only once CI actually ran on unrestricted infrastructure and are
fixed on `main`: `gradlew` had lost its executable bit when committed from
Windows (`Permission denied`, exit 126, invisible locally since git-bash's
`./gradlew` invocation doesn't enforce the mode bit the same way), and the
integration test's original assertions collided with the 1082 real players
`SyncScheduler` seeds into the same fresh database before the test methods
run (`@SpringBootTest` boots the *full* app, bootstrap logic included) — see
the git history for both.

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
- **`vision`**'s HUD calibration and real-capture accuracy are unverified —
  see the dedicated "Vision pipeline" section above for exactly what is and
  isn't proven, and why.
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
