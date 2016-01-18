package com.github.cs4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Cron scheduler implementation. Use Scheduler::schedule method to enable scheduling for all methods
 * annotated with Scheduled annotation.
 */
public class Scheduler implements AutoCloseable {

    /**
     * List of managed tasks.
     */
    @NotNull
    private final List<Task> tasks = new ArrayList<>();

    /**
     * Scheduling thread. No tasks run on this thread.
     */
    @NotNull
    private final ScheduledExecutorService manager = new ScheduledThreadPoolExecutor(1);

    /**
     * Task executors. No management is performed on these threads.
     */
    @NotNull
    private final ExecutorService workers;

    @Nullable
    private EventLogger eventLogger;

    /**
     * Instantiates new Scheduler initialized with executor service
     * created by {@link Executors#newFixedThreadPool(int)} call with nThreads.
     * <p/>
     * Initial delay is 5 seconds. Periodical check interval is 5 seconds.
     *
     * @param nThreads number of threads in pool.
     */
    public Scheduler(int nThreads) {
        this(Executors.newFixedThreadPool(nThreads), 5, 5, TimeUnit.SECONDS);
    }

    public Scheduler(@NotNull ExecutorService s, int initialDelay, int checkInterval, TimeUnit timeUnit) {
        this.manager.scheduleAtFixedRate((Runnable) new Runnable() {
            @Override
            public void run() {
                doSchedule();
            }
        }, initialDelay, checkInterval, timeUnit);
        this.workers = s;
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
                        synchronized (tasks) {
                            tasks.add(new Task(obj, m, new CronSequenceGenerator(annotation.cron())));
                        }
                    }
                }
            } catch (SecurityException ignored) {
                break;
            }
        }
    }

    private void doSchedule() {
        synchronized (tasks) {
            long currentMillis = System.currentTimeMillis();
            for (Task t : tasks) {
                if (t.nextExecutingTime >= currentMillis) {
                    continue;
                }
                if (t.executing) {
                    t.nextExecutingTime = t.sequenceGenerator.next(currentMillis);
                    continue;
                }
                try {
                    t.lastExecutingTime = System.currentTimeMillis();
                    workers.execute(t);
                    t.executing = true;
                } catch (RejectedExecutionException e) {
                    if (eventLogger != null) {
                        eventLogger.onError("Failed to start task: " + t, e);
                    }
                }
            }
        }
    }

    public void shutdown() {
        manager.shutdown();
        workers.shutdown();
    }

    public boolean isShutdown() {
        return manager.isShutdown();
    }

    public void setEventLogger(@Nullable EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    public void close() {
        shutdown();
    }

    private class Task implements Runnable {
        @NotNull
        final Object instance;
        @NotNull
        final Method method;
        @NotNull
        final CronSequenceGenerator sequenceGenerator;

        volatile long lastExecutingTime = 0;
        volatile long nextExecutingTime = 0;
        volatile boolean executing;

        public Task(@NotNull Object instance, @NotNull Method method, @NotNull CronSequenceGenerator sequenceGenerator) {
            this.instance = instance;
            this.method = method;
            this.sequenceGenerator = sequenceGenerator;
        }


        public void run() {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                if (eventLogger != null) {
                    eventLogger.onError("Exception in task: " + this, e);
                }
            } finally {
                nextExecutingTime = sequenceGenerator.next(System.currentTimeMillis());
                executing = false;
            }
        }
    }


}
