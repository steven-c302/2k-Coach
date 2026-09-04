package com.nba2kassistant.core.roster;

import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerRepository;
import com.nba2kassistant.core.roster.dto.BuildAroundCriterionSpec;
import com.nba2kassistant.core.roster.dto.OverallRangeCriterionSpec;
import com.nba2kassistant.core.roster.dto.PositionCriterionSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.nba2kassistant.core.roster.PlayerFixtures.player;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RosterCriterionFactoryTest {

    @Mock
    private PlayerRepository playerRepository;

    @Test
    void resolvesEachSpecTypeToItsMatchingCriterion() {
        Player anchor = player(42, "Anchor", "PG", 90);
        when(playerRepository.findById(42L)).thenReturn(Optional.of(anchor));

        RosterCriterionFactory factory = new RosterCriterionFactory(playerRepository);
        RosterCriterionFactory.Resolution resolution = factory.resolve(List.of(
                new OverallRangeCriterionSpec(80, 99),
                new PositionCriterionSpec(List.of("PG", "SG")),
                new BuildAroundCriterionSpec(42L)
        ));

        assertThat(resolution.criteria()).hasSize(3);
        assertThat(resolution.criteria().get(0)).isInstanceOf(OverallRangeCriterion.class);
        assertThat(resolution.criteria().get(1)).isInstanceOf(PositionCriterion.class);
        assertThat(resolution.criteria().get(2)).isInstanceOf(BuildAroundPlayerCriterion.class);
        assertThat(resolution.anchor()).isEqualTo(anchor);
    }

    @Test
    void throwsWhenBuildAroundPlayerDoesNotExist() {
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        RosterCriterionFactory factory = new RosterCriterionFactory(playerRepository);

        assertThatThrownBy(() -> factory.resolve(List.of(new BuildAroundCriterionSpec(999L))))
                .isInstanceOf(RosterGenerationException.class);
    }

    @Test
    void resolvesEmptySpecListToNoAnchorAndNoCriteria() {
        RosterCriterionFactory factory = new RosterCriterionFactory(playerRepository);

        RosterCriterionFactory.Resolution resolution = factory.resolve(List.of());

        assertThat(resolution.criteria()).isEmpty();
        assertThat(resolution.anchor()).isNull();
    }
}
