package com.nba2kassistant.core.sync;

/** Matches the shape of nba2k-data-scraper's players.json exactly (name/team/position/overall only). */
record ScraperPlayer(String name, String team, String position, short overall) {
}
