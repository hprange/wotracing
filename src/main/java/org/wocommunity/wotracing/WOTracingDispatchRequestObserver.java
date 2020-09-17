package org.wocommunity.wotracing;

import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSNotification;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * <a href="mailto:hprange@gmail.com">Henrique Prange</a>
 */
public class WOTracingDispatchRequestObserver {
    public static final String DEFAULT_OPERATION_NAME = "webobjects";
    private final ThreadLocal<Scope> scopeStorage = new ThreadLocal<>();
    private final Tracer tracer;
    private final String operation;

    public WOTracingDispatchRequestObserver(Tracer tracer) {
        this(tracer, DEFAULT_OPERATION_NAME);
    }

    public WOTracingDispatchRequestObserver(Tracer tracer, String operation) {
        this.tracer = tracer;
        this.operation = operation;
    }

    /**
     * @param notification
     */
    public void onRequest(NSNotification notification) {
        WORequest request = (WORequest) notification.object();

        Span span = tracer.buildSpan(operation).start();

        Scope scope = tracer.activateSpan(span);

        scopeStorage.set(scope);

        span.setTag(Tags.HTTP_METHOD, request.method());
        span.setTag(Tags.HTTP_URL, request.uri());
    }

    /**
     * @param notification
     */
    public void onResponse(NSNotification notification) {
        WOResponse response = (WOResponse) notification.object();

        Span span = tracer.activeSpan();

        span.setTag(Tags.HTTP_STATUS, response.status());

        span.finish();
        scopeStorage.get().close();
        scopeStorage.remove();
    }
}
