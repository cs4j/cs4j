package com.github.cs4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cron scheduler implementation. Use Scheduler::schedule method to enable scheduling for all methods
 * annotated with Scheduled annotation.
 */
public class Scheduler implements AutoCloseable {

    /**
     * Stub used when no actual logger instance is set/
     */
    private static final EventLogger NULL_LOGGER = new EventLogger() {
    };

    /**
     * Object used for internal locking/notifications.
     */
    private final Object monitor = new Object();

    /**
     * List of managed tasks.
     */
    @NotNull
    private final List<SchedulerTask> tasks = new ArrayList<>();

    /**
     * Task executor instance.
     */
    @NotNull
    public final ExecutorService tasksExecutor;

    @NotNull
    EventLogger eventLogger = NULL_LOGGER;

    /**
     * Thread that makes all scheduling job.
     */
    @NotNull
    public final SchedulerThread schedulerThread;

    /**
     * If scheduler is active or not.
     */
    private boolean active;

    public Scheduler(@NotNull ExecutorService tasksExecutor, int initialDelay, int checkInterval, @NotNull TimeUnit timeUnit, @NotNull String schedulerThreadName) {
        schedulerThread = new SchedulerThread(initialDelay, checkInterval, timeUnit, schedulerThreadName);
        this.tasksExecutor = tasksExecutor;
        active = true;
        schedulerThread.start();
    }


    public void schedule(@NotNull Object obj) {
        for (Class<?> cls = obj.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            // processing all methods, not only public ones in order to detect potential errors
            // earlier during initialization phase.
            try {
                for (Method m : cls.getDeclaredMethods()) {
                    Scheduled annotation = m.getAnnotation(Scheduled.class);
                    if (annotation != null) {
                        int mod = m.getModifiers();
                        if (!Modifier.isPublic(mod)) {
                            throw new IllegalArgumentException("Method is private: " + m);
                        }
                        if (m.getGenericParameterTypes().length != 0) {
                            throw new IllegalArgumentException("Method has non zero parameters: " + m);
                        }
                        synchronized (monitor) {
                            tasks.add(new SchedulerTask(this, obj, m, new CronSequenceGenerator(annotation.cron())));
                        }
                    }
                }
            } catch (SecurityException ignored) {
                break;
            }
        }
    }

    private void checkAndExecute() {
        eventLogger.onCheckInterval();
        synchronized (monitor) {
            long currentMillis = System.currentTimeMillis();
            for (SchedulerTask t : tasks) {
                if (t.nextExecutingTime >= currentMillis) {
                    continue;
                }
                if (t.executing) {
                    t.nextExecutingTime = t.sequenceGenerator.next(currentMillis);
                    continue;
                }
                try {
                    t.lastExecutingTime = System.currentTimeMillis();
                    eventLogger.onBeforeExecute(t);
                    tasksExecutor.execute(t);
                    t.executing = true;
                } catch (RejectedExecutionException e) {
                    eventLogger.onError("Failed to start task: " + t, e);
                }
            }
        }
    }

    public void shutdown() {
        active = false;
        synchronized (monitor) {
            monitor.notify();
        }
        tasksExecutor.shutdown();
    }

    public boolean isShutdown() {
        return !active;
    }

    public void setEventLogger(@Nullable EventLogger eventLogger) {
        this.eventLogger = eventLogger == null ? NULL_LOGGER : eventLogger;
    }

    @SuppressWarnings("unused")
    @NotNull
    public List<SchedulerTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    @Override
    public void close() {
        shutdown();
    }

    public class SchedulerThread extends Thread {
        public final int initialDelay;
        public final int checkInterval;
        public final TimeUnit timeUnit;

        private SchedulerThread(int initialDelay, int checkInterval, TimeUnit timeUnit, @NotNull String threadName) {
            if (initialDelay < 0) {
                throw new IllegalArgumentException("initialDelay < 0. Value: " + initialDelay);
            }
            if (checkInterval <= 0) {
                throw new IllegalArgumentException("checkInterval must be > 0. Value: " + checkInterval);
            }
            if (timeUnit == null) {
                throw new IllegalArgumentException("timeUnit is null");
            }
            this.initialDelay = initialDelay;
            this.checkInterval = checkInterval;
            this.timeUnit = timeUnit;
            setName(threadName);
            setDaemon(true);
        }

        @Override
        public void run() {
            if (initialDelay > 0) {
                pause(timeUnit.toMillis(initialDelay));
            }
            long checkIntervalMillis = timeUnit.toMillis(checkInterval);
            while (active) {
                try {
                    checkAndExecute();
                    pause(checkIntervalMillis);
                } catch (Exception e) {
                    System.err.println("Got internal error that must never happen!");
                    e.printStackTrace();
                }
            }
        }

        private void pause(long checkIntervalMillis) {
            try {
                synchronized (monitor) {
                    monitor.wait(checkIntervalMillis);
                }
            } catch (InterruptedException e) {
                System.err.println("Got unexpected interrupted exception! Ignoring");
                e.printStackTrace();
            }
        }
    }
}
