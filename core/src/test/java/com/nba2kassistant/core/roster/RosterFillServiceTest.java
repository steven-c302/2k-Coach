package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixed-fixture test for the ported create_rosters_with_starters logic —
 * the "full-pipeline" test named in NBA2K Assistant plan §8, exercising the
 * fill algorithm the way the roster-generation service actually drives it
 * (an already filtered/ordered candidate list, not raw DB rows).
 */
class RosterFillServiceTest {

    private final RosterFillService service = new RosterFillService();

    @Test
    void recomputesBestOverallPerPositionAcrossTheWholeRosterNotJustTheInitialPick() {
        Player pgA = player(1, "PG_A", "PG", 80);
        Player sgA = player(2, "SG_A", "SG", 80);
        Player sfA = player(3, "SF_A", "SF", 80);
        Player pfA = player(4, "PF_A", "PF", 80);
        Player cA = player(5, "C_A", "C", 80);
        Player pgB = player(6, "PG_B", "PG", 95); // better PG, arrives via bench fill
        Player sgB = player(7, "SG_B", "SG", 70);
        Player sfB = player(8, "SF_B", "SF", 70);

        List<Player> ordered = List.of(pgA, sgA, sfA, pfA, cA, pgB, sgB, sfB);
        RosterBuildContext ctx = new RosterBuildContext(8, "CURRENT", null);

        GeneratedRoster result = service.fill(ordered, ordered, ctx);

        assertThat(result.starters()).extracting(Player::getName)
                .containsExactly("PG_B", "SG_A", "SF_A", "PF_A", "C_A");
        assertThat(result.bench()).extracting(Player::getName)
                .containsExactly("PG_A", "SG_B", "SF_B");
    }

    @Test
    void buildAroundAnchorStaysStarterEvenWhenABetterSamePositionPlayerIsAvailable() {
        Player anchor = player(1, "Anchor", "PG", 70);
        Player sgA = player(2, "SG_A", "SG", 80);
        Player sfA = player(3, "SF_A", "SF", 80);
        Player pfA = player(4, "PF_A", "PF", 80);
        Player cA = player(5, "C_A", "C", 80);
        Player strongPg = player(6, "StrongPG", "PG", 95);

        List<Player> ordered = List.of(sgA, sfA, pfA, cA, strongPg); // anchor excluded, as BuildAroundPlayerCriterion would do
        RosterBuildContext ctx = new RosterBuildContext(6, "CURRENT", anchor);

        GeneratedRoster result = service.fill(ordered, ordered, ctx);

        assertThat(result.starters()).extracting(Player::getName)
                .containsExactly("Anchor", "SG_A", "SF_A", "PF_A", "C_A");
        assertThat(result.bench()).extracting(Player::getName).containsExactly("StrongPG");
    }

    @Test
    void throwsWhenNoPlayerFillsARequiredPosition() {
        List<Player> ordered = List.of(
                player(1, "PG_A", "PG", 80),
                player(2, "SG_A", "SG", 80),
                player(3, "SF_A", "SF", 80),
                player(4, "PF_A", "PF", 80)
                // no center
        );
        RosterBuildContext ctx = new RosterBuildContext(5, "CURRENT", null);

        assertThatThrownBy(() -> service.fill(ordered, ordered, ctx))
                .isInstanceOf(RosterGenerationException.class)
                .hasMessageContaining("C");
    }

    @Test
    void throwsWhenTeamSizeIsOutOfBounds() {
        RosterBuildContext ctx = new RosterBuildContext(16, "CURRENT", null);

        assertThatThrownBy(() -> service.fill(List.of(), List.of(), ctx))
                .isInstanceOf(RosterGenerationException.class);
    }
}
