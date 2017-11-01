package com.github.cs4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Callback for exception from scheduled method.
 */
public interface EventLogger {
    default void onCheckInterval() {
    }

    default void onError(@NotNull String message, @Nullable Exception e) {
    }

    default void onBeforeExecute(@NotNull SchedulerTask task) {
    }
}
