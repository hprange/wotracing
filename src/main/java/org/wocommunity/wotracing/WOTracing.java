package org.wocommunity.wotracing;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.NSNotificationCenter;
import er.extensions.ERXExtensions;
import er.extensions.ERXFrameworkPrincipal;
import er.extensions.foundation.ERXProperties;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import static er.extensions.foundation.ERXSelectorUtilities.notificationSelector;
import static java.util.Objects.requireNonNull;
import static org.wocommunity.wotracing.WOTracingDispatchRequestObserver.DEFAULT_OPERATION_NAME;

/**
 * @author <a href="mailto:hprange@gmail.com">Henrique Prange</a>
 */
public class WOTracing extends ERXFrameworkPrincipal {
    public final static Class<?>[] REQUIRES = new Class[]{ERXExtensions.class};

    /**
     * Option to enable/disable WOTracing. (default: true)
     */
    public static final String WOTRACING_ENABLED = "wotracing.enabled";

    /**
     * Option to change the operation name of {@code Span}s created to track request-response cycles. (default: webobjects)
     */
    public static final String WOTRACING_OPERATION_NAME = "wotracing.operationName";

    protected static WOTracing sharedInstance;

    static {
        setUpFrameworkPrincipalClass(WOTracing.class);
    }

    public static WOTracing sharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = sharedInstance(WOTracing.class);
        }

        return sharedInstance;
    }

    private final Tracer tracer;
    private final boolean isTracingEnabled;
    private WOTracingDispatchRequestObserver observer;

    public WOTracing() {
        this(GlobalTracer.get());
    }

    public WOTracing(Tracer tracer) {
        requireNonNull(tracer, "Cannot initialize WOTracing with null Tracer. Please, provide a OpenTracing compatible implementation.");

        this.tracer = tracer;
        this.isTracingEnabled = ERXProperties.booleanForKeyWithDefault(WOTRACING_ENABLED, true);
    }


    @Override
    protected void initialize() {
        super.initialize();

        if (isTracingEnabled) {
            String operation = ERXProperties.stringForKeyWithDefault(WOTRACING_OPERATION_NAME, DEFAULT_OPERATION_NAME);

            observer = new WOTracingDispatchRequestObserver(tracer, operation);
        }
    }

    @Override
    public void finishInitialization() {
        if (isTracingEnabled) {
            NSNotificationCenter notificationCenter = NSNotificationCenter.defaultCenter();

            notificationCenter.addObserver(observer, notificationSelector("onRequest"), WOApplication.ApplicationWillDispatchRequestNotification, null);
            notificationCenter.addObserver(observer, notificationSelector("onResponse"), WOApplication.ApplicationDidDispatchRequestNotification, null);
        }
    }
}
