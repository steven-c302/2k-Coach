package com.nba2kassistant.core.matchup;

import com.nba2kassistant.core.badge.Badge;
import com.nba2kassistant.core.badge.PlayerBadge;
import com.nba2kassistant.core.badge.PlayerBadgeRepository;
import com.nba2kassistant.core.player.Player;
import com.nba2kassistant.core.player.PlayerAttributes;
import com.nba2kassistant.core.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamSnapshotFactoryTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerBadgeRepository playerBadgeRepository;

    @Test
    void buildsASnapshotWithAttributesAndBadgesAttached() {
        Player player = new Player();
        player.setId(1L);
        player.setName("Test Player");
        player.setPosition("SG");
        PlayerAttributes attrs = new PlayerAttributes();
        attrs.setThreePt((short) 90);
        player.setAttributes(attrs);

        Badge badge = new Badge();
        badge.setSlug("clamps");
        badge.setCategory("Defense");
        PlayerBadge playerBadge = new PlayerBadge(player, badge, (short) 3);

        when(playerRepository.findAllById(List.of(1L))).thenReturn(List.of(player));
        when(playerBadgeRepository.findByIdPlayerIdIn(List.of(1L))).thenReturn(List.of(playerBadge));

        TeamSnapshotFactory factory = new TeamSnapshotFactory(playerRepository, playerBadgeRepository);
        TeamSnapshot snapshot = factory.build("Team A", List.of(1L));

        assertThat(snapshot.players()).hasSize(1);
        PlayerSnapshot playerSnapshot = snapshot.players().get(0);
        assertThat(playerSnapshot.attributes().threePt()).isEqualTo((short) 90);
        assertThat(playerSnapshot.badges()).hasSize(1);
        assertThat(playerSnapshot.badges().get(0).slug()).isEqualTo("clamps");
    }

    @Test
    void leavesAttributesNullWhenPlayerHasNoAttributesRow() {
        Player player = new Player();
        player.setId(2L);
        player.setName("No Attributes");
        player.setPosition("C");

        when(playerRepository.findAllById(List.of(2L))).thenReturn(List.of(player));
        when(playerBadgeRepository.findByIdPlayerIdIn(List.of(2L))).thenReturn(List.of());

        TeamSnapshotFactory factory = new TeamSnapshotFactory(playerRepository, playerBadgeRepository);
        TeamSnapshot snapshot = factory.build("Team A", List.of(2L));

        assertThat(snapshot.players().get(0).attributes()).isNull();
        assertThat(snapshot.players().get(0).badges()).isEmpty();
    }

    @Test
    void throwsWhenAPlayerIdDoesNotExist() {
        when(playerRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(new Player()));

        TeamSnapshotFactory factory = new TeamSnapshotFactory(playerRepository, playerBadgeRepository);

        assertThatThrownBy(() -> factory.build("Team A", List.of(1L, 999L)))
                .isInstanceOf(MatchupAnalysisException.class);
    }
}
