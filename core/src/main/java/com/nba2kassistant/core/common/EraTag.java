package com.nba2kassistant.core.common;

import java.time.Year;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The local scraper's {@code team} field encodes era as a string prefix, e.g.
 * {@code "'96 CHI"} for a classic team vs. {@code "Chicago Bulls"} for a current
 * one (see NBA2K Assistant plan §9). This pulls that apart into a clean
 * {@code (team, eraTag)} pair instead of carrying the encoding into the schema.
 */
public final class EraTag {

    public static final String CURRENT = "CURRENT";

    private static final Pattern CLASSIC_TEAM = Pattern.compile("^'(\\d{2})\\s+([A-Z]{2,4})$");

    private EraTag() {
    }

    public record Parsed(String team, String eraTag) {
    }

    /** Two-digit years are resolved against the current year: 26 or lower is 20xx, else 19xx. */
    public static Parsed parse(String rawTeam) {
        return parse(rawTeam, Year.now().getValue() % 100);
    }

    static Parsed parse(String rawTeam, int centuryPivot) {
        if (rawTeam == null) {
            return new Parsed(null, CURRENT);
        }
        Matcher matcher = CLASSIC_TEAM.matcher(rawTeam.trim());
        if (!matcher.matches()) {
            return new Parsed(rawTeam.trim(), CURRENT);
        }
        int twoDigitYear = Integer.parseInt(matcher.group(1));
        int century = twoDigitYear <= centuryPivot ? 2000 : 1900;
        String abbreviation = matcher.group(2);
        return new Parsed(abbreviation, String.valueOf(century + twoDigitYear));
    }
}
