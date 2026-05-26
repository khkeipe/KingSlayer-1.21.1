package com.koreykeipe.kingslayer.game;

import javax.annotation.Nullable;
import java.util.List;

public record KillEntry(
    String victimName,
    @Nullable String killerName,
    String cause,
    boolean indirect,
    List<AssistEntry> assists,
    long timestamp
) {}
