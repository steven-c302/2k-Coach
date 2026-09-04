package com.nba2kassistant.core.matchup;

import java.util.List;

/**
 * Immutable input to the coaching rules engine (NBA2K Assistant plan §5).
 * {@code label} identifies the team in {@link Mismatch#favoredTeam()} and in
 * evidence strings — deliberately a plain string (e.g. "Team A"), not a
 * roster/session id, so rules stay pure functions over data with no
 * knowledge of where the teams came from.
 */
public record TeamSnapshot(String label, List<PlayerSnapshot> players) {
}
