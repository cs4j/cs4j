package com.github.cs4j;

import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;

public class SchedulerTask implements Runnable {

    @NotNull
    public final Scheduler scheduler;

    @NotNull
    public final Object instance;

    @NotNull
    public final Method method;

    @NotNull
    public final CronSequenceGenerator sequenceGenerator;

    volatile long lastExecutingTime = 0;
    volatile long nextExecutingTime = 0;
    volatile boolean executing;

    public SchedulerTask(@NotNull Scheduler scheduler, @NotNull Object instance, @NotNull Method method, @NotNull CronSequenceGenerator sequenceGenerator) {
        this.scheduler = scheduler;
        this.instance = instance;
        this.method = method;
        this.sequenceGenerator = sequenceGenerator;
    }


    public void run() {
        try {
            method.invoke(instance);
        } catch (Exception e) {
            scheduler.eventLogger.onError("Exception in task: " + this, e);
        } finally {
            nextExecutingTime = sequenceGenerator.next(System.currentTimeMillis());
            executing = false;
        }
    }

    public long getLastExecutingTime() {
        return lastExecutingTime;
    }

    public long getNextExecutingTime() {
        return nextExecutingTime;
    }

    public boolean isExecuting() {
        return executing;
    }

    @Override
    public String toString() {
        return "SchedulerTask[" + instance + "" + method + "]";
    }
}
