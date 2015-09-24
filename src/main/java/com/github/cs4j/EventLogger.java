package com.github.cs4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public interface EventLogger {
    void onError(@NotNull String message, @Nullable Exception e);
}
