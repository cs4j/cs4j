package com.github.cs4j.asset;

import com.github.cs4j.Scheduled;

/**
 *
 */
public class SampleService1 {

    public static volatile int staticCounter = 0;
    public int counter = 0;

    private final Callback callback;

    public SampleService1(Callback callback) {
        this.callback = callback;
    }

    @Scheduled(cron = "* * * * * *")
    public void tick() {
        counter++;
        staticCounter++;
        callback.callback(this);
    }

    public interface Callback {
        void callback(SampleService1 service);
    }
}
