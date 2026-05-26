package com.koreykeipe.kingslayer.game;

/**
 * Represents a player who contributed to a kill without landing the final blow.
 * contribution is a human-readable label: "34% damage", "trap_kill", "raid_summon", etc.
 */
public record AssistEntry(String playerName, String contribution) {}
