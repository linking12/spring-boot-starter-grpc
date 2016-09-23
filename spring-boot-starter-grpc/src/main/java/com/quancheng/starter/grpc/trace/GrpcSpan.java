package com.quancheng.starter.grpc.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import io.opentracing.Span;
import io.opentracing.SpanContext;

public final class GrpcSpan implements Span {

    private static AtomicLong         nextId     = new AtomicLong(0);

    private final GrpcTracer          mockTracer;
    private GrpcSpanContext           context;
    private final long                parentId;
    private final long                startMicros;
    private long                      finishMicros;
    private final Map<String, Object> tags;
    private final List<LogEntry>      logEntries = new ArrayList<>();
    private String                    operationName;

    GrpcSpan(GrpcTracer tracer, String operationName, long startMicros, Map<String, Object> initialTags,
             GrpcSpanContext parent){
        this.mockTracer = tracer;
        this.operationName = operationName;
        this.startMicros = startMicros;
        if (initialTags == null) {
            this.tags = new HashMap<>();
        } else {
            this.tags = new HashMap<>(initialTags);
        }
        if (parent == null) {
            this.context = new GrpcSpanContext(nextId(), nextId(), new HashMap<String, String>());
            this.parentId = 0;
        } else {
            this.context = new GrpcSpanContext(parent.traceId, nextId(), parent.baggage);
            this.parentId = parent.spanId;
        }
    }

    public String operationName() {
        return this.operationName;
    }

    public long parentId() {
        return parentId;
    }

    public long startMicros() {
        return startMicros;
    }

    public long finishMicros() {
        assert finishMicros > 0 : "must call finish() before finishMicros()";
        return finishMicros;
    }

    public Map<String, Object> tags() {
        return new HashMap<>(this.tags);
    }

    public List<LogEntry> logEntries() {
        return new ArrayList<>(this.logEntries);
    }

    @Override
    public synchronized GrpcSpanContext context() {
        return this.context;
    }

    @Override
    public void finish() {
        this.finish(System.nanoTime() / 1000);
    }

    @Override
    public synchronized void finish(long finishMicros) {
        this.finishMicros = finishMicros;
        this.mockTracer.appendFinishedSpan(this);
    }

    @Override
    public void close() {
        this.finish();
    }

    @Override
    public synchronized Span setTag(String key, String value) {
        this.tags.put(key, value);
        return this;
    }

    @Override
    public synchronized Span setTag(String key, boolean value) {
        this.tags.put(key, value);
        return this;
    }

    @Override
    public synchronized Span setTag(String key, Number value) {
        this.tags.put(key, value);
        return this;
    }

    @Override
    public Span log(String eventName, Object payload) {
        return this.log(System.nanoTime() / 1000, eventName, payload);
    }

    @Override
    public synchronized Span log(long timestampMicroseconds, String eventName, Object payload) {
        this.logEntries.add(new LogEntry(timestampMicroseconds, eventName, payload));
        return this;
    }

    @Override
    public synchronized Span setBaggageItem(String key, String value) {
        this.context = this.context.withBaggageItem(key, value);
        return this;
    }

    @Override
    public synchronized String getBaggageItem(String key) {
        return this.context.getBaggageItem(key);
    }

    public static final class GrpcSpanContext implements SpanContext {

        private final long                traceId;
        private final Map<String, String> baggage;
        private final long                spanId;

        GrpcSpanContext(long traceId, long spanId, Map<String, String> baggage){
            this.baggage = baggage;
            this.traceId = traceId;
            this.spanId = spanId;
        }

        public String getBaggageItem(String key) {
            return this.baggage.get(key);
        }

        public long traceId() {
            return traceId;
        }

        public long spanId() {
            return spanId;
        }

        public GrpcSpanContext withBaggageItem(String key, String val) {
            Map<String, String> newBaggage = new HashMap<>(this.baggage);
            newBaggage.put(key, val);
            return new GrpcSpanContext(this.traceId, this.spanId, newBaggage);
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            return baggage.entrySet();
        }
    }

    public static final class LogEntry {

        private final long   timestampMicros;
        private final String eventName;
        private final Object payload;

        public LogEntry(long timestampMicros, String eventName, Object payload){
            this.timestampMicros = timestampMicros;
            this.eventName = eventName;
            this.payload = payload;
        }

        public long timestampMicros() {
            return timestampMicros;
        }

        public String eventName() {
            return eventName;
        }

        public Object payload() {
            return payload;
        }
    }

    static long nextId() {
        return nextId.addAndGet(1);
    }
}
