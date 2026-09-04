package com.nba2kassistant.core.player;

import java.util.List;

public record PlayerResponse(
        Long id,
        String name,
        String team,
        String position,
        List<String> positions,
        Short overall,
        String eraTag,
        String source,
        PlayerAttributesResponse attributes
) {

    public static PlayerResponse from(Player player) {
        PlayerAttributesResponse attrs = player.getAttributes() == null ? null
                : PlayerAttributesResponse.from(player.getAttributes());
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getTeam(),
                player.getPosition(),
                player.getPositions(),
                player.getOverall(),
                player.getEraTag(),
                player.getSource(),
                attrs
        );
    }

    record PlayerAttributesResponse(
            Short threePt,
            Short midRange,
            Short layup,
            Short dunk,
            Short speed,
            Short strength,
            Short postDefense,
            Short perimeterDefense
    ) {
        static PlayerAttributesResponse from(PlayerAttributes a) {
            return new PlayerAttributesResponse(
                    a.getThreePt(), a.getMidRange(), a.getLayup(), a.getDunk(),
                    a.getSpeed(), a.getStrength(), a.getPostDefense(), a.getPerimeterDefense()
            );
        }
    }
}
