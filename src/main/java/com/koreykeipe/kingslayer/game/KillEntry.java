package com.koreykeipe.kingslayer.game;

import javax.annotation.Nullable;

public record KillEntry(
    String victimName,
    @Nullable String killerName,
    String cause,
    boolean indirect,
    long timestamp
) {}
