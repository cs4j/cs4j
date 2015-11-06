package com.github.cs4j;

import com.github.cs4j.asset.SampleService1;
import com.github.cs4j.asset.SampleService2;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.cs4j.asset.SampleService1.Callback;

/**
 *
 */
public class SchedulerTest extends Assert {
    private static final Object SYNC_OBJECT = new Object();


    @Test
    public void checkSchedulerCheckSingleThread() throws InterruptedException {
        try (final Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(1), 0, 1, TimeUnit.SECONDS)) {
            final int nIterations = 3;

            SampleService1 service = new SampleService2(new Callback() {
                public void callback(SampleService1 service) {
                    if (service.counter == nIterations) {
                        scheduler.shutdown();
                        synchronized (SYNC_OBJECT) {
                            SYNC_OBJECT.notify();
                        }
                    }
                }
            });
            assertEquals(0, service.counter);
            long t0 = System.currentTimeMillis();
            scheduler.schedule(service);

            synchronized (SYNC_OBJECT) {
                SYNC_OBJECT.wait(5 * 1000L);
            }
            int counter = service.counter;
            long totalTime = System.currentTimeMillis() - t0;

            assertEquals(nIterations, counter);
            assertTrue(scheduler.isShutdown());
            assertTrue(totalTime < 4 * 1000);

            Thread.sleep(2000L);
            assertEquals(counter, service.counter);
        }
    }

    @Test
    public void checkSchedulerCheckMultipleThreads() throws InterruptedException {
        try (Scheduler scheduler = new Scheduler(Executors.newFixedThreadPool(10), 0, 1, TimeUnit.SECONDS)) {
            SampleService1.staticCounter = 0;
            final int nIterations = 30;

            Callback c = new Callback() {
                public void callback(SampleService1 service) {
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
    public void checkPrivateMethodThrowsException() throws InterruptedException {
        try (Scheduler scheduler = new Scheduler(1)) {
            scheduler.schedule(new Object() {
                @Scheduled
                private void foo() {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkProtectedMethodThrowsException() throws InterruptedException {
        try (Scheduler scheduler = new Scheduler(1)) {
            scheduler.schedule(new Object() {
                @Scheduled
                protected void foo() {
                }
            });
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkLocalMethodThrowsException() throws InterruptedException {
        try (Scheduler scheduler = new Scheduler(1)) {
            scheduler.schedule(new Object() {
                @Scheduled
                void foo() {
                }
            });
        }
    }
}
