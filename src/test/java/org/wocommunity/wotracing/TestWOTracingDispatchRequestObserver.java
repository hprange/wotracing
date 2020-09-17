package org.wocommunity.wotracing;

import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSNotification;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * <a href="mailto:hprange@gmail.com">Henrique Prange</a>
 */
@ExtendWith(MockitoExtension.class)
class TestWOTracingDispatchRequestObserver {
    @Mock
    NSNotification requestNotification;

    @Mock
    WORequest request;

    MockTracer tracer;

    WOTracingDispatchRequestObserver observer;

    @BeforeEach
    public void setup() {
        tracer = new MockTracer();

        when(requestNotification.object()).thenReturn(request);

        observer = new WOTracingDispatchRequestObserver(tracer);
    }

    @Nested
    class OnRequest {
        @Test
        void activateSpan() {
            observer.onRequest(requestNotification);

            MockSpan span = (MockSpan) tracer.activeSpan();

            assertThat(span, notNullValue());
            assertThat(span.operationName(), is("webobjects"));
        }

        @Test
        void setHttpMethodAndUrl() {
            when(request.method()).thenReturn("GET");
            when(request.uri()).thenReturn("https://...");

            observer.onRequest(requestNotification);

            MockSpan span = (MockSpan) tracer.activeSpan();

            assertThat(span.tags(), hasEntry("http.method", "GET"));
            assertThat(span.tags(), hasEntry("http.url", "https://..."));
        }

        @Test
        void activateSpanWithCustomOperationName() {
            observer = new WOTracingDispatchRequestObserver(tracer, "custom operation name");

            observer.onRequest(requestNotification);

            MockSpan span = (MockSpan) tracer.activeSpan();

            assertThat(span.operationName(), is("custom operation name"));
        }
    }

    @Nested
    class OnResponse {
        @Mock
        WOResponse response;

        @Mock
        NSNotification responseNotification;

        @BeforeEach
        public void setup() {
            when(responseNotification.object()).thenReturn(response);

            observer.onRequest(requestNotification);
        }

        @Test
        void setHttpStatus() {
            MockSpan span = (MockSpan) tracer.activeSpan();

            when(response.status()).thenReturn(200);

            observer.onResponse(responseNotification);

            assertThat(span.tags(), hasEntry("http.status_code", 200));
        }

        @Test
        void finishSpan() {
            MockSpan span = (MockSpan) tracer.activeSpan();

            observer.onResponse(responseNotification);

            assertThat(tracer.finishedSpans(), hasItem(span));
        }

        @Test
        void closeScope() {
            observer.onResponse(responseNotification);

            assertThat(tracer.scopeManager().activeSpan(), nullValue());
        }
    }
}
