package com.github.cs4j;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Cron scheduler implementation. Use Scheduler::schedule method to enable scheduling for all methods
 * annotated with Scheduled annotation.
 */
public class Scheduler implements Closeable {

    @NotNull
    private final List<Task> tasks = new ArrayList<>();

    @NotNull
    private final ScheduledExecutorService manager = new ScheduledThreadPoolExecutor(1);

    @NotNull
    private final ScheduledExecutorService workers;

    public Scheduler(int nThreads) {
        this(Executors.newScheduledThreadPool(nThreads), 5, 5, TimeUnit.SECONDS);
    }

    public Scheduler(@NotNull ScheduledExecutorService s, int initialDelay, int checkInterval, TimeUnit timeUnit) {
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
                if (t.nextSchedulingTime >= currentMillis) {
                    continue;
                }
                if (t.scheduled) {
                    t.nextSchedulingTime = t.sequenceGenerator.next(currentMillis);
                    continue;
                }
                try {
                    t.lastSchedulingTime = System.currentTimeMillis();
                    workers.schedule(t, 0, TimeUnit.NANOSECONDS);
                    t.scheduled = true;
                } catch (RejectedExecutionException e) {
                    //todo:
                }
            }
        }
    }

    public void close() {
        manager.shutdown();
        workers.shutdown();
    }

    private class Task implements Runnable {
        @NotNull
        final Object instance;
        @NotNull
        final Method method;
        @NotNull
        final CronSequenceGenerator sequenceGenerator;

        volatile long lastSchedulingTime = 0;
        volatile long nextSchedulingTime = 0;
        volatile boolean scheduled;

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
                nextSchedulingTime = sequenceGenerator.next(System.currentTimeMillis());
                scheduled = false;
            }
        }
    }
}
