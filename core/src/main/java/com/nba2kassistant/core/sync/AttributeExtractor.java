package com.nba2kassistant.core.sync;

import com.nba2kassistant.core.player.PlayerAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Projects the nba2kapi {@code attributes} blob (nested by category, e.g.
 * {@code shooting.threePointShot}) onto the 8 columns the coaching rules
 * engine reads (Milestone 4). Searches every nested object for the first
 * matching field name rather than assuming a fixed category layout, since
 * only the "shooting" category's field names are confirmed from the docs.
 */
final class AttributeExtractor {

    private static final Map<String, String[]> FIELD_CANDIDATES = Map.of(
            "threePt", new String[]{"threePointShot", "three_point_shot", "threePt"},
            "midRange", new String[]{"midRangeShot", "mid_range_shot", "midRange"},
            "layup", new String[]{"drivingLayup", "layup", "closeShot"},
            "dunk", new String[]{"drivingDunk", "standingDunk", "dunk"},
            "speed", new String[]{"speed"},
            "strength", new String[]{"strength"},
            "postDefense", new String[]{"interiorDefense", "postDefense", "post_defense"},
            "perimeterDefense", new String[]{"perimeterDefense", "perimeter_defense"}
    );

    private AttributeExtractor() {
    }

    static void apply(PlayerAttributes target, JsonNode attributesNode, ObjectMapper objectMapper) {
        if (attributesNode == null || attributesNode.isMissingNode() || attributesNode.isNull()) {
            return;
        }
        target.setThreePt(findFirst(attributesNode, FIELD_CANDIDATES.get("threePt")));
        target.setMidRange(findFirst(attributesNode, FIELD_CANDIDATES.get("midRange")));
        target.setLayup(findFirst(attributesNode, FIELD_CANDIDATES.get("layup")));
        target.setDunk(findFirst(attributesNode, FIELD_CANDIDATES.get("dunk")));
        target.setSpeed(findFirst(attributesNode, FIELD_CANDIDATES.get("speed")));
        target.setStrength(findFirst(attributesNode, FIELD_CANDIDATES.get("strength")));
        target.setPostDefense(findFirst(attributesNode, FIELD_CANDIDATES.get("postDefense")));
        target.setPerimeterDefense(findFirst(attributesNode, FIELD_CANDIDATES.get("perimeterDefense")));
        target.setRawAttributes(objectMapper.convertValue(attributesNode, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {
        }));
    }

    private static Short findFirst(JsonNode root, String[] candidateNames) {
        JsonNode hit = search(root, candidateNames);
        return hit == null || !hit.isNumber() ? null : hit.shortValue();
    }

    private static JsonNode search(JsonNode node, String[] candidateNames) {
        if (node == null || !node.isContainerNode()) {
            return null;
        }
        for (String candidate : candidateNames) {
            if (node.has(candidate)) {
                return node.get(candidate);
            }
        }
        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            JsonNode found = search(children.next(), candidateNames);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
