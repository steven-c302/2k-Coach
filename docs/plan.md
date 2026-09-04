# Court Vision — Implementation Plan

## Context

Steven's resume includes a "goal" bullet for **Court Vision — Live Matchup Tool**, claiming a full Java/Spring Boot + WebSockets + PostgreSQL + DynamoDB + Docker + GitHub Actions system with versioned async LLM strategy generation. In reality, only two small, disconnected Python scripts exist today under `nba2k-roster-project/`:

- `nba2k-data-scraper` — BeautifulSoup scraper of 2kratings.com → `data/players.json` (1082 players, name/team/position/overall only, no per-attribute ratings or badges).
- `nba2k-roster-randomizer` — a terminal `input()`/`print()` CLI that filters a 100-player subset by position/overall and randomly builds rosters with a "best-overall-per-position starter" rule. No server, no tests, no web layer.

The goal is to close this gap for real: build Court Vision into a genuinely extensive, working project for the friend group's own NBA 2K26/27 sessions (random/criteria-based team building, pregame matchup analysis, live head-to-head sessions, and real-time in-game coaching), sized and structured to be a standout project for big-tech and enterprise SWE internship applications (SAS, Wells Fargo, Bandwidth, and similar). Steven explicitly wants to understand the *why* behind each technical decision so he can own this project in interviews, not just have it built for him — the roadmap below is sequenced as a learning path, not just a feature list.

**Confirmed decisions driving this plan** (from clarifying questions):
- Backend stays Java + Spring Boot + WebSockets (matches resume, differentiated enterprise-Java story vs. typical Node/Python portfolios) — but a **separate Python microservice** handles computer vision, since that ecosystem (OpenCV/EasyOCR) fits far better than Java.
- Dual database split (PostgreSQL for structured player/roster data, DynamoDB for append-heavy history/event logs) is kept, but must be justified as a real architectural decision, not resume cargo-culting.
- Local/Docker-Compose first; AWS deployment is an explicit later phase, not part of this build.
- Live "what's happening right now" coaching input: Steven plays NBA 2K on **PC**, offline "Play Now" mode only (no online play, so no anti-cheat/ToS concern with observing the screen). No public game-state API exists, and **memory/RAM reading was explicitly considered and rejected** as the mechanism (fragile across patches, real risk of tripping anti-tamper even offline, not a transferable resume skill, high maintenance burden). Chosen approach: **real-time OCR via screen capture** — legal, patch-resilient at the pixel level, and a genuine, resume-worthy CV/systems skill. This is hybridized with a manual tap-tracker (reusing the interaction pattern from Steven's existing `pickup-stats` project) for event-level detail OCR can't reliably extract.
- Player attribute/badge data: use the `nba2kapi` project (https://github.com/wkoverfield/nba2kapi) — a hosted REST API sourced from 2kratings.com exposing player attributes, badges, and team rosters (current/classic/all-time), MIT licensed, free API keys (~100 req/hr auth / 60 req/min public) — as the primary structured data source, synced periodically into Court Vision's own Postgres rather than hot-pathed. The existing local scraper (name/team/position/overall only) is a fallback if nba2kapi coverage proves insufficient. **No ML is needed or justified for player ratings** — that data already exists structurally; ML only reappears later as an honest stretch goal (a win-probability model trained on Court Vision's own accumulated matchup history, once there's enough personal-use data).

---

## 1. System Architecture

**Four pieces:**

1. **`courtvision-core`** (Java 21 + Spring Boot) — the source of truth. Owns Postgres and DynamoDB access, exposes REST for team-building and matchup analysis, exposes WebSocket/STOMP for live sessions, runs the coaching rules engine, and orchestrates async LLM narration calls.
2. **`courtvision-vision`** (Python + FastAPI, OpenCV/EasyOCR + a capture library) — watches the game window and POSTs structured events (`{type: "score_update", ...}`) to core. It has **no game-domain logic** — it only emits observed facts; all "what does this mean" reasoning stays in Java.
3. **`courtvision-web`** (Next.js/React/TypeScript) — team builder UI, live session view, coaching feed, manual tap-tracker (adapted from `pickup-stats`).
4. **Postgres** (structured master data) + **DynamoDB via LocalStack locally** (append-heavy history/event data).

**Why this shape:** The vision pipeline is a genuinely different runtime problem — OpenCV/EasyOCR/screen-capture libraries are Python-native; forcing that into the JVM means fighting the ecosystem for no benefit. This is a **language-fit split**, not microservices-for-its-own-sake. Everything else (rosters, sessions, matchup analysis, coaching rules, persistence) stays in one Spring Boot deployable — team builder, session manager, and coaching engine share the same data and transaction boundaries, so splitting them further would only add network hops and distributed-consistency pain for a single-team project. The vision service is deliberately **downstream and dumb**: it never touches Postgres/DynamoDB directly, so Java core remains the single authoritative writer to both databases — no dual-write races, no ownership ambiguity.

**Communication:** Web↔Core over REST (CRUD, team-building, matchup analysis) and WebSocket/STOMP (live session + coaching feed). Vision↔Core starts as REST POST per detected event (simpler to test/curl/debug) — upgrade to a WebSocket push only if REST latency is visibly a bottleneck once V1 is working.

---

## 2. Data Model

**Postgres** (relational — team-building queries are joins + range filters, e.g. "PG, overall 85–95, three_pt > 80, not already rostered," which is exactly what Postgres indexes well):

- `players(id, name, team, position, overall, era_tag, source, last_synced_at)`
- `player_attributes(player_id FK, three_pt, mid_range, layup, dunk, speed, strength, post_defense, perimeter_defense, ...)` — populated once the nba2kapi sync (Milestone 1) lands; the current scraper only has `overall`.
- `badges(id, name, category, tier_levels)` + `player_badges(player_id FK, badge_id FK, tier)` — needed for badge-based mismatch rules (e.g. "Clamps").
- `teams(id, name, abbreviation, era_tag)` for real rosters, distinct from user-generated fantasy rosters.
- `rosters(id, owner_session_id, name, era_tag, created_at)` + `roster_players(roster_id FK, player_id FK, is_starter, position_slot)`.
- `sessions(id, code, state, host_client_id, guest_client_id, created_at, version)` — `version` is the same monotonic counter used for LLM staleness detection (§5), persisted so a restart doesn't lose it mid-session.

**DynamoDB** (high-write, append-mostly, single-partition access — a poor fit for Postgres's relational strengths):

- **`MatchupHistory`** — PK `sessionId`, SK `timestamp#eventType`. One append per completed matchup: `{sessionId, timestamp, teamA_snapshot, teamB_snapshot, finalScore, mismatchSummary}`.
- **`CoachingEventLog`** — PK `sessionId`, SK `sequenceNumber` (the §5 version counter). `{sessionId, sequenceNumber, requestedAt, respondedAt, ruleEngineOutput, llmNarration, status: DELIVERED|DISCARDED_STALE}`. **This table is the interview artifact for the async/versioning resume claim** — being able to query it and show discarded-stale entries is concrete, demoable proof, not just a code claim.

The split, stated plainly: *structured, queryable, low-write data → Postgres; append-heavy, single-partition-key, unstructured-payload event data → DynamoDB.* That's the real reasoning to give in an interview, not "the resume said so."

---

## 3. Team-Builder Module

Port the **logic shape** of the existing Python filters/randomizer into Java, generalized from two hardcoded functions into a composable strategy:

```java
public interface RosterCriterion {
    List<Player> apply(List<Player> candidates, RosterBuildContext ctx);
}
```

Implementations: `OverallRangeCriterion`, `PositionCriterion`, `EraCriterion`, and a new `BuildAroundPlayerCriterion` (not in the current Python version) — given an anchor player, computes the roster's positional/statistical needs using the *same* attribute-delta logic the coaching engine uses in §5 (deliberate shared code, not duplication). Starter/bench selection ports `create_rosters_with_starters`'s "best overall per position starts" rule directly — sound logic, just needs a Java rewrite with JUnit fixtures.

```
POST /api/rosters/generate
  { criteria: [{type: "OVERALL_RANGE", min, max}, {type:"POSITION", allowed:[...]},
               {type: "BUILD_AROUND", playerId}], teamSize, era }
GET  /api/players?position=PG&minOverall=85&maxOverall=99&era=CURRENT
```

---

## 4. Concurrency Design for Live Sessions

**Chosen pattern: per-session single-writer actor, not a shared mutable map + locks.**

Each active session gets a dedicated task-queue executor (single-thread, or a virtual-thread-per-session executor — Java 21 makes "one thread per session" cheap at this scale). All mutations (roster picks, ready state, matchup requests) go through that session's queue, so only one thread ever touches a given session's fields — correctness by construction, no lock-ordering to reason about. The **only** genuinely shared concurrent structure is a top-level `ConcurrentHashMap<String, SessionActor>` (session code → actor), used exactly for what it's good at (independent-key lookup/insert) — explicitly *not* used to hold session state directly, since a plain concurrent map doesn't protect multi-field invariants like "roster + ready-flag must move together."

WebSocket/STOMP frames route by session code to that session's actor queue; broadcasts (`SimpMessagingTemplate`) fire after the actor task produces a new immutable state snapshot.

**Why this over raw locking (the interview answer):** *"I wanted linearizable per-session state changes without reasoning about lock ordering across multiple fields that must move together; a single-writer-per-session model gives that for free."* The session's `version` counter increments on every actor-processed mutation — and is the **same counter** used for LLM staleness detection below, so there's one monotonic source of truth, not two that can drift.

---

## 5. Coaching Rules Engine + Async/Versioned LLM Narration

**Rules engine first (deterministic, testable, no LLM/network):** `MismatchRule` interface, each rule takes two `TeamSnapshot`s and returns `Mismatch(category, severity, evidence)`. Example rules:

- `ThreePointVolumeRule` — teamA avg `three_pt` − teamB avg `perimeter_defense` > threshold → `SHOOT_MORE_THREES`.
- `PostSizeRule` — post-position `post_defense`/`strength` differential → `ATTACK_INSIDE`.
- `SpeedMismatchRule` — `speed` vs `lateral_quickness` differential → `DRIVE_MORE`.
- `BadgeExploitRule` — opposing perimeter defender holds "Clamps" tier ≥2 and your ball-handler lacks a comparable dribble-move badge → `AVOID_ISOLATION_VS_X`.

Pure functions over structured input — trivially JUnit-tested with fixed `TeamSnapshot` fixtures, no mocks needed. This is what makes "explainable matchup analysis" true rather than aspirational.

**Async/versioned LLM narration** (implements the resume's own claim literally):

1. Session holds an `AtomicLong currentVersion` (shared with §4's actor version).
2. On new game state, the actor increments the version, captures `long requestVersion`, and submits:
   ```java
   CompletableFuture
     .supplyAsync(() -> llmClient.narrate(mismatches), llmExecutor)
     .thenAccept(narration -> {
         if (session.currentVersion.get() != requestVersion) {
             log.discard(sessionId, requestVersion, "stale"); return;
         }
         session.actor.submit(() -> broadcastNarration(narration));
     });
   ```
3. `llmExecutor` is a **dedicated** bounded/virtual-thread pool, separate from the session-actor pool — isolates slow LLM I/O from the low-latency game-state path (a deliberate choice, not the default `ForkJoinPool`).
4. Every request/response — including discards — is written to `CoachingEventLog` (§2), making the claim auditable, not just code-true.
5. V2 refinement (not required for V1 correctness): cancel in-flight LLM calls on staleness, not just discard on return.

---

## 6. OCR / Vision Pipeline

**Why not memory/RAM reading:** fragile across patches (offsets shift every update), platform/build-specific, real risk of tripping anti-tamper even offline, not a transferable CV/systems skill, high maintenance burden for a portfolio project. Screen-capture OCR is legal, resilient at the pixel level (HUD layout is stable within a resolution, unlike memory offsets), and directly showcases OpenCV skills that generalize.

**Pipeline (PC, no capture card needed):**

1. **Capture** — `mss` for V1 (simple, portable); note `bettercam`/`dxcam` (Desktop Duplication API) as a drop-in perf upgrade later, since swapping the capture library doesn't touch downstream code.
2. **Preprocess (OpenCV)** — crop to fixed HUD regions (score, clock, shot clock — calibrated per resolution, stored as a config profile), grayscale, threshold, upscale 2–4x to help OCR read 2K's stylized font. This step is where most of the accuracy work happens — budget real iteration time here.
3. **OCR** — EasyOCR for V1 (better out-of-box accuracy on stylized fonts than Tesseract, pure-Python packaging); note the accuracy-vs-latency-vs-dependency-weight tradeoff against Tesseract explicitly as a measured decision.
4. **Emit** — `POST /api/vision/events {sessionId, type: SCORE_UPDATE|CLOCK_UPDATE|BOX_SCORE_UPDATE, payload, capturedAt}`, polled every 1–2s (values change slowly; no need for frame-rate OCR).

**V1 scope (reliable, commit to this):** score, quarter, game clock, shot clock, box-score overlay (FG%, 3PM/3PA, rebounds) when visible — large, high-contrast, fixed-position elements, well within generic OCR's reliability envelope.

**Explicitly deferred (do not commit to for V1):** shot-by-shot/zone-level detection, on-court player identification, possession tracking — these need object detection/pose estimation, a different and much harder CV problem.

**Hybrid manual tap-tracker:** reuse the *interaction pattern* (not code) from `pickup-stats/index.html` — tap-to-increment stat buttons, undo-by-popping-last-entry — rebuilt as a React component in `courtvision-web`, posting to the same event-shaped endpoint so OCR-derived and manually-tapped events flow through one unified stream into the coaching engine.

---

## 7. Milestone Roadmap

Sequenced so fundamentals (CRUD, tests, WebSockets) are solid before the hardest CV work; every milestone is independently demoable and independently resume-truthful.

1. **Data foundation** — Postgres schema, nba2kapi sync job (or extended scraper fallback), seed `players`/`player_attributes`/`badges`. *Demo: `GET /api/players` returns real attribute-rich data.*
2. **Team builder API** — `RosterCriterion` pipeline + REST, JUnit-tested. *Demo: generate a roster via curl with composed criteria including "build around player."*
3. **Next.js frontend for team builder** — form → roster display. *Demo: full user-facing flow.*
4. **Matchup rules engine (no LLM yet)** — `MismatchRule`s, `POST /api/matchups/analyze`. Heaviest JUnit coverage of the project — prioritize it.
5. **Live sessions over WebSocket** — session-code join, per-session actor concurrency (§4), broadcast state. *Demo: two browser tabs sharing live session state.*
6. **Async/versioned LLM narration** — wire LLM onto milestone 4's output, implement version-check-and-discard, log to DynamoDB/LocalStack. *Demo: force a race, show a discarded-stale entry in `CoachingEventLog`. This is the single most resume-critical milestone.*
7. **DynamoDB matchup history** — persist completed matchups, simple history view.
8. **Dockerize + docker-compose** — Postgres, Spring Boot, Next.js, LocalStack, one `docker-compose up`.
9. **GitHub Actions CI** — JUnit + Testcontainers on push/PR.
10. **Vision microservice V1** — `courtvision-vision` capturing score/clock/box-score, emitting to core; manual tap-tracker as fallback/complement. *Demo: play a real game, watch live score/clock update in the session view. Deliberately last — depends on everything else already working and tested, and is the hardest, most differentiating piece.*

**Explicit stretch/V2 (out of this plan's scope):** AWS deployment, win-probability ML model trained on accumulated DynamoDB matchup history, shot-zone/event-level CV.

---

## 8. Testing Strategy

- **Rules engine** — pure unit tests, fixture `TeamSnapshot`s per rule, no mocks. Highest value-to-effort ratio in the project.
- **Team builder** — unit tests per `RosterCriterion` + a full-pipeline integration test against a fixed fixture set.
- **Persistence layer** — Testcontainers-backed Postgres integration tests for query-shape-sensitive logic (position/overall/badge filters) — don't mock the DB here.
- **Session concurrency** — spin up N simulated concurrent mutations against one session actor, assert the final state equals sequential application. This is the test that actually *proves* the thread-safety claim.
- **LLM versioning** — the highest resume-value test: simulate response A delayed, response B (version bump) arriving first, then A resolving — assert A is discarded and logged `DISCARDED_STALE`. Use a fake/controllable-delay LLM client, not a real API call.
- **Vision pipeline** — `pytest` against a fixture set of captured screenshots with known ground-truth values (golden-file style); not run live-game in CI (too flaky).
- **CI** — GitHub Actions running JUnit + Testcontainers from Milestone 4 onward, kept green continuously, not bolted on at the end.

---

## 9. Reuse Decisions

**Port logic shape (Python → Java):**
- [`nba2k-roster-randomizer/src/filters.py`](../../Projects/nba2k-roster-project/nba2k-roster-randomizer/src/filters.py) → `OverallRangeCriterion`/`PositionCriterion`.
- [`nba2k-roster-randomizer/src/randomizer.py`](../../Projects/nba2k-roster-project/nba2k-roster-randomizer/src/randomizer.py) → starter/bench selection algorithm, same test-fixture shape.

**Reuse interaction pattern, not code:**
- [`pickup-stats/index.html`](../../Projects/pickup-stats/index.html) → manual tap-tracker React component (different framework, same UX shape).

**Rebuild from scratch:** `nba2k-roster-randomizer/main.py` (CLI loop) and `src/display.py` (console output) — fully replaced by REST + Next.js.

**Data source decision:** evaluate nba2kapi first (primary); [`nba2k-data-scraper/scripts/scrape_ratings.py`](../../Projects/nba2k-roster-project/nba2k-data-scraper/scripts/scrape_ratings.py) is the fallback if coverage/rate limits are insufficient — note it currently only parses list/team pages (name/team/position/overall), not individual player pages where attributes/badges actually live, so "extend" means real new scraping work. The existing [`players.json`](../../Projects/nba2k-roster-project/nba2k-data-scraper/data/players.json) (1082 players) is reusable as a schema-bootstrap seed, but its `team` field encodes era as a string prefix (e.g. `"'96 CHI"`) that should be cleaned into the new `era_tag` column, not carried over as-is.

---

## Development Environment

Do primary development on the **PC** (RTX 3060 Ti, 32GB RAM), not the M3 MacBook (8GB RAM):

- The full local stack — Postgres, LocalStack (DynamoDB), a Spring Boot JVM, a Next.js dev server, and EasyOCR (torch-based) — comfortably fits in 32GB and will likely thrash/swap on 8GB once run concurrently via `docker-compose up`.
- EasyOCR can use CUDA; the 3060 Ti gives a real inference speedup during the OCR accuracy/latency tuning in Milestone 10, which the Mac can't match (no CUDA, inconsistent MPS support).
- If using the Mac for comfort/portability, connect via **VS Code Remote-SSH (or Remote Tunnels)**, not remote-desktop — this remotes only the editor/terminal/build tooling to the PC, leaving screen capture untouched (it reads the PC's own local display buffer regardless of an SSH session). Don't use a remote-desktop *video* session (e.g. Parsec, Windows Remote Desktop) to both play and develop at once — for actual gameplay and OCR testing, sit at the PC directly, since a remote-desktop video stream adds input lag and risks the capture pipeline reading compressed/re-encoded pixels instead of the native buffer if not configured carefully.

## Verification

Each milestone above names its own demo. End-to-end verification of the full system: run `docker-compose up` (Milestone 8+), open two browser tabs to `courtvision-web`, join the same session code, generate rosters via the team builder, trigger a matchup analysis and confirm coaching narration appears (and that forcing rapid re-analysis produces a visible discarded-stale entry queryable in DynamoDB/LocalStack), then start `courtvision-vision` alongside a real NBA 2K "Play Now" game on PC and confirm live score/clock events appear in the session view. JUnit + Testcontainers suite (`./gradlew test` or `mvn test`) and the GitHub Actions workflow should stay green throughout.
