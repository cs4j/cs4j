package com.github.cs4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler granularity is 5 seconds.
 */
public class Scheduler implements Closeable {

    private final List<Task> tasks = new ArrayList<>();
    private final List<Task> runningTasks = new ArrayList<>();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

    public Scheduler() {
        scheduler.scheduleAtFixedRate((Runnable) new Runnable() {
            @Override
            public void run() {
                Scheduler.this.doSchedule();
            }
        }, 10, 5, TimeUnit.SECONDS);
    }

    public void schedule(Object obj) {
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
                if (t.executorThread != null) {
                    continue;
                }
                if (t.nextExecutionTime < currentMillis) {
                    t.start();
                }
            }
        }
    }

    public void close() {
        synchronized (runningTasks) {
            runningTasks.forEach(new Consumer<Task>() {
                @Override
                public void accept(Task task) {
                    task.shutdown();
                }
            });
        }
        scheduler.shutdown();
    }

    private class Task implements Runnable {
        @NotNull
        final Object instance;
        @NotNull
        final Method method;
        @NotNull
        final CronSequenceGenerator sequenceGenerator;

        long lastExecutionTime = 0;
        long nextExecutionTime = 0;

        @Nullable
        volatile Thread executorThread;

        public Task(@NotNull Object instance, @NotNull Method method, @NotNull CronSequenceGenerator sequenceGenerator) {
            this.instance = instance;
            this.method = method;
            this.sequenceGenerator = sequenceGenerator;
        }

        public synchronized void start() {
            if (executorThread != null) {
                throw new IllegalStateException("Already running : " + this);
            }
            Thread t = new Thread(this);
            executorThread = t;
            lastExecutionTime = System.currentTimeMillis();
            runningTasks.add(this);
            t.start();
        }

        public synchronized void shutdown() {
            Thread t = executorThread;
            if (t != null) {
                executorThread = null;
                t.interrupt();
            }
        }

        public void run() {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                //TODO: log.error("", e);
            } finally {
                synchronized (this) {
                    nextExecutionTime = sequenceGenerator.next(System.currentTimeMillis());
                    executorThread = null;
                }
            }
        }
    }
}
