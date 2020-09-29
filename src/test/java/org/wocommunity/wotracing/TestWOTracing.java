package org.wocommunity.wotracing;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSNotificationCenter;
import er.extensions.ERXFrameworkPrincipalHelper;
import er.extensions.foundation.ERXProperties;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * <a href="mailto:hprange@gmail.com">Henrique Prange</a>
 */
@ExtendWith(MockitoExtension.class)
class TestWOTracing {
    @BeforeAll
    public static void skipWOApplicationInitialization() {
        ERXFrameworkPrincipalHelper.skipWOApplicationInitialization();
    }

    @Mock
    WORequest request;
    MockTracer tracer;
    WOTracing tracing;

    @BeforeEach
    public void setup() {
        ERXProperties.setStringForKey("true", "wotracing.enabled");

        tracer = new MockTracer();

        tracing = new WOTracing(tracer);
    }

    @Test
    void observeRequestDispatching() {
        tracing.initialize();
        tracing.finishInitialization();

        postNotification(WOApplication.ApplicationWillDispatchRequestNotification, request);

        MockSpan span = (MockSpan) tracer.activeSpan();

        assertThat(span, notNullValue());

        WOResponse response = mock(WOResponse.class);

        postNotification(WOApplication.ApplicationDidDispatchRequestNotification, response);

        assertThat(tracer.finishedSpans(), hasItem(span));
    }

    @Test
    void observeRequestDispatchingWithCustomOperationName() {
        ERXProperties.setStringForKey("custom operation", "wotracing.operationName");

        try {
            tracing.initialize();
            tracing.finishInitialization();

            postNotification(WOApplication.ApplicationWillDispatchRequestNotification, request);

            MockSpan span = (MockSpan) tracer.activeSpan();

            assertThat(span.operationName(), is("custom operation"));
        } finally {
            ERXProperties.removeKey("wotracing.operationName");
        }
    }

    @Test
    void doNotObserveRequestDispatchingIfWOTracingDisabled() {
        ERXProperties.setStringForKey("false", "wotracing.enabled");

        try {
            tracing = new WOTracing(tracer);

            tracing.initialize();
            tracing.finishInitialization();

            postNotification(WOApplication.ApplicationWillDispatchRequestNotification, request);

            Span span = tracer.activeSpan();

            assertThat(span, nullValue());
        } finally {
            ERXProperties.removeKey("wotracing.enabled");
        }
    }

    @Test
    void throwExceptionWhenTracerIsNull() {
        Exception exception = assertThrows(NullPointerException.class, () -> new WOTracing(null));

        assertThat(exception.getMessage(), is("Cannot initialize WOTracing with null Tracer. Please, provide a OpenTracing compatible implementation."));
    }

    private void postNotification(String notificationName, Object object) {
        NSNotificationCenter.defaultCenter().postNotification(notificationName, object);
    }
}