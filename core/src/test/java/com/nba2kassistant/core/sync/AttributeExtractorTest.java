package com.nba2kassistant.core.sync;

import com.nba2kassistant.core.player.PlayerAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void projectsKnownFieldsFromNestedCategoriesRegardlessOfDepth() throws Exception {
        JsonNode attributes = objectMapper.readTree("""
                {
                  "shooting": {"closeShot": 80, "midRangeShot": 89, "threePointShot": 99},
                  "athleticism": {"speed": 91, "strength": 70},
                  "defense": {"interiorDefense": 60, "perimeterDefense": 75}
                }
                """);

        PlayerAttributes target = new PlayerAttributes();
        AttributeExtractor.apply(target, attributes, objectMapper);

        assertThat(target.getThreePt()).isEqualTo((short) 99);
        assertThat(target.getMidRange()).isEqualTo((short) 89);
        assertThat(target.getSpeed()).isEqualTo((short) 91);
        assertThat(target.getStrength()).isEqualTo((short) 70);
        assertThat(target.getPostDefense()).isEqualTo((short) 60);
        assertThat(target.getPerimeterDefense()).isEqualTo((short) 75);
        assertThat(target.getRawAttributes()).containsKey("shooting");
    }

    @Test
    void missingCategoryLeavesFieldNullInsteadOfThrowing() throws Exception {
        JsonNode attributes = objectMapper.readTree("""
                { "shooting": {"threePointShot": 99} }
                """);

        PlayerAttributes target = new PlayerAttributes();
        AttributeExtractor.apply(target, attributes, objectMapper);

        assertThat(target.getThreePt()).isEqualTo((short) 99);
        assertThat(target.getSpeed()).isNull();
    }
}
