package com.koreykeipe.kingslayer.game;

import java.util.UUID;

/**
 * Represents a player who contributed to a kill without landing the final blow.
 * {@code contribution} is a human-readable label: "34% damage", "trap_kill", "wind_blast", etc.
 * The UUID lets the game reward the assister (threat/score), not just name them in the feed.
 */
public record AssistEntry(UUID playerUUID, String playerName, String contribution) {}
