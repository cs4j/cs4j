package com.github.cs4j;

import com.github.cs4j.asset.SampleService1;
import com.github.cs4j.asset.SampleService2;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import static com.github.cs4j.asset.SampleService1.Callback;

public class SchedulerTest extends Assert {

    private static final Object SYNC_OBJECT = new Object();

    @Test
    public void checkSchedulerCheckSingleThread() throws InterruptedException {
        try (final Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 1, TimeUnit.SECONDS, "T1")) {
            final int nIterations = 3;

            SampleService1 service = new SampleService2(s -> {
                System.out.println("Iteration: " + s.counter);
                if (s.counter == nIterations) {
                    scheduler.shutdown();
                    System.out.println("Scheduler is shut down!");
                    synchronized (SYNC_OBJECT) {
                        SYNC_OBJECT.notify();
                    }
                }
            });
            assertEquals(0, service.counter);
            long t0 = System.currentTimeMillis();
            scheduler.schedule(service);

            synchronized (SYNC_OBJECT) {
                SYNC_OBJECT.wait(5_000L);
            }
            int counter = service.counter;
            long totalTime = System.currentTimeMillis() - t0;

            assertEquals(nIterations, counter);
            assertTrue(scheduler.isShutdown());
            assertTrue(totalTime < 4_000);

            Thread.sleep(2_000L);
            assertEquals(counter, service.counter);
        }
    }

    @Test
    public void checkSchedulerCheckMultipleThreads() throws InterruptedException {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(10), 0, 1, TimeUnit.SECONDS, "T2")) {
            SampleService1.staticCounter = 0;
            final int nIterations = 30;

            Callback c = s -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (SampleService1.staticCounter == nIterations) {
                    synchronized (SYNC_OBJECT) {
                        SYNC_OBJECT.notify();
                    }
                }
            };
            synchronized (SYNC_OBJECT) {
                for (int i = 0; i < 10; i++) {
                    scheduler.schedule(new SampleService1(c));
                }
                SYNC_OBJECT.wait(5 * 1000L);
            }
            scheduler.shutdown();
            assertTrue("Number of iterations: " + SampleService1.staticCounter + ">=" + nIterations, SampleService1.staticCounter >= nIterations);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkPrivateMethodThrowsException() {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T3")) {
            scheduler.schedule(new Object() {
                @Scheduled
                private void foo() {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkProtectedMethodThrowsException() {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T4")) {
            scheduler.schedule(new Object() {
                @Scheduled
                protected void foo() {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkLocalMethodThrowsException() {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T5")) {
            scheduler.schedule(new Object() {
                @Scheduled
                void foo() {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkMethodWithParametersThrowsException1() {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T6")) {
            scheduler.schedule(new Object() {
                @Scheduled
                public void foo(int x) {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkMethodWithParametersThrowsException2() {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T7")) {
            scheduler.schedule(new Object() {
                @Scheduled
                public void foo(Object... args) {
                }
            });
        }
    }


    @Test
    public void checkScheduledPeriodsOverlap() throws InterruptedException {
        final int callCostInSeconds = 3;
        final AtomicInteger count = new AtomicInteger();
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 200, TimeUnit.MILLISECONDS, "T10")) {
            scheduler.setEventLogger(new EventLogger() {
                @Override
                public void onError(@NotNull String message, @Nullable Exception e) {
                    System.err.println(message);
                    if (e != null) {
                        e.printStackTrace();
                    }
                }
            });
            Object scheduled = new Object() {
                @Scheduled(cron = "* * * * * *")
                public void foo() throws InterruptedException {
                    int cycle = count.incrementAndGet();
                    System.out.println("cycle: " + cycle + " started");
                    Thread.sleep(callCostInSeconds * 1000);
                    System.out.println("cycle: " + count.get() + " finished");
                }
            };
            scheduler.schedule(scheduled);

            int nCyclesToCheck = 5;
            Thread.sleep(nCyclesToCheck * callCostInSeconds * 1000L);
            int minCyclesExpected = nCyclesToCheck - 1;
            Assert.assertTrue("At least " + minCyclesExpected + " cycles must be started at this point, actual: " + count.get(),
                    count.get() == nCyclesToCheck || count.get() == nCyclesToCheck - 1);
        }
    }
}
