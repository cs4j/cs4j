package com.github.cs4j;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
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
public class Scheduler {

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
        for (Method m : obj.getClass().getMethods()) {
            Scheduled annotation = m.getAnnotation(Scheduled.class);
            if (annotation != null) {
                synchronized (tasks) {
                    tasks.add(new Task(obj, m, new CronSequenceGenerator(annotation.cron())));
                }
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
                    //todo:
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
                //TODO: log.error("", e);
            } finally {
                nextExecutingTime = sequenceGenerator.next(System.currentTimeMillis());
                executing = false;
            }
        }
    }
}
