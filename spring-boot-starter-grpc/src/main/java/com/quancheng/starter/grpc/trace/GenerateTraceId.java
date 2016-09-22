package com.quancheng.starter.grpc.trace;

import java.util.concurrent.atomic.AtomicLong;

public class GenerateTraceId {

    private static class LazyHolder {

        private static final GenerateTraceId INSTANCE = new GenerateTraceId();
    }

    public static final GenerateTraceId getInstance() {
        return LazyHolder.INSTANCE;
    }

    private GenerateTraceId(){

    }

    private Long       seed     = 0L;
    private Long       MAX_STEP = 0xffffffL;
    private AtomicLong plusId   = new AtomicLong(0L);

    public Long getTraceId() {
        return (seed << 40) | getPlusId();
    }

    private long getPlusId() {
        if (plusId.get() >= MAX_STEP) {
            plusId.set(0L);
        }
        return plusId.incrementAndGet();
    }

}
