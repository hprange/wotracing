package er.extensions;

/**
 * Helper class to access {@code ERXFrameworkPrincipal} internals.
 *
 * @author <a href="mailto:hprange@gmail.com">Henrique Prange</a>
 */
public class ERXFrameworkPrincipalHelper {
    /**
     * Skips the {@code WOApplication} initialization by simulating the initialization
     * of the {@code WOTracing} class without actually initializing it.
     */
    public static void skipWOApplicationInitialization() {
        ERXFrameworkPrincipal.initializedFrameworks.put("org.wocommunity.wotracing.WOTracing", new ERXFrameworkPrincipal() {
            @Override
            public void finishInitialization() {
            }
        });
    }
}
