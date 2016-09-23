package com.quancheng.starter.grpc.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public final class GrpcTracer implements Tracer {

    private static final Logger log           = LoggerFactory.getLogger(GrpcTracer.class);

    private List<GrpcSpan>      finishedSpans = new ArrayList<>();
    private final Propagator    propagator;

    public GrpcTracer(){
        this(Propagator.PRINTER);
    }

    public GrpcTracer(Propagator propagator){
        this.propagator = propagator;
    }

    public synchronized void reset() {
        this.finishedSpans.clear();
    }

    public synchronized List<GrpcSpan> finishedSpans() {
        return new ArrayList<>(this.finishedSpans);
    }

    public interface Propagator {

        <C> void inject(GrpcSpan.GrpcSpanContext ctx, Format<C> format, C carrier);

        <C> GrpcSpan.GrpcSpanContext extract(Format<C> format, C carrier);

        Propagator PRINTER = new Propagator() {

            @Override
            public <C> void inject(GrpcSpan.GrpcSpanContext ctx, Format<C> format, C carrier) {
                log.debug("inject(" + ctx + ", " + format + ", " + carrier + ")");
            }

            @Override
            public <C> GrpcSpan.GrpcSpanContext extract(Format<C> format, C carrier) {
                log.debug("extract(" + format + ", " + carrier + ")");
                return null;
            }
        };
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new SpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        this.propagator.inject((GrpcSpan.GrpcSpanContext) spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return this.propagator.extract(format, carrier);
    }

    synchronized void appendFinishedSpan(GrpcSpan mockSpan) {
        this.finishedSpans.add(mockSpan);
    }

    final class SpanBuilder implements Tracer.SpanBuilder {

        private final String             operationName;
        private long                     startMicros;
        private GrpcSpan.GrpcSpanContext firstParent;
        private Map<String, Object>      initialTags = new HashMap<>();

        SpanBuilder(String operationName){
            this.operationName = operationName;
        }

        @Override
        public Tracer.SpanBuilder asChildOf(SpanContext parent) {
            return addReference(References.CHILD_OF, parent);
        }

        @Override
        public Tracer.SpanBuilder asChildOf(Span parent) {
            return addReference(References.CHILD_OF, parent.context());
        }

        @Override
        public Tracer.SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            if (firstParent == null
                && (referenceType.equals(References.CHILD_OF) || referenceType.equals(References.FOLLOWS_FROM))) {
                this.firstParent = (GrpcSpan.GrpcSpanContext) referencedContext;
            }
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, String value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, boolean value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, Number value) {
            this.initialTags.put(key, value);
            return this;
        }

        @Override
        public Tracer.SpanBuilder withStartTimestamp(long microseconds) {
            this.startMicros = microseconds;
            return this;
        }

        @Override
        public Span start() {
            return new GrpcSpan(GrpcTracer.this, this.operationName, this.startMicros, initialTags, this.firstParent);
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            if (firstParent == null) {
                return Collections.EMPTY_MAP.entrySet();
            } else {
                return firstParent.baggageItems();
            }
        }
    }
}
