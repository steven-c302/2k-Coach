package com.nba2kassistant.core.matchup;

import java.util.List;

/**
 * A pure, deterministic matchup check (NBA2K Assistant plan §5) — no LLM, no
 * network, trivially testable against fixed {@link TeamSnapshot} fixtures.
 * Returns 0-2 mismatches: most rules check both directions (does A exploit B,
 * does B exploit A) independently.
 */
public interface MismatchRule {

    List<Mismatch> evaluate(TeamSnapshot teamA, TeamSnapshot teamB);
}
