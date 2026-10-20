package ai.mutuus.common.agent.transport;

import io.opentelemetry.javaagent.extension.instrumentation.*;
import java.util.List;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import static net.bytebuddy.matcher.ElementMatchers.*;

/** Dev opt-in for the pinned JDK ABI; replaces java-http-client, never runs alongside it. */
public class JdkTransportModule extends InstrumentationModule {
    public JdkTransportModule() { super("mutuus-jdk-transport"); }
    @Override public boolean defaultEnabled() { return false; }
    @Override public boolean isHelperClass(String name) { return name.startsWith("ai.mutuus.common.agent.transport.JdkTransportState"); }
    @Override public List<String> getAdditionalHelperClassNames() {
        return List.of("ai.mutuus.common.agent.transport.JdkTransportState", "ai.mutuus.common.agent.transport.JdkTransportState$Attempt");
    }
    @Override public List<TypeInstrumentation> typeInstrumentations() {
        return List.of(new Hook("MultiExchange", "<init>", "Call"), new Hook("MultiExchange", "responseAsync", "Cleanup"),
            new Hook("Exchange", "responseAsync", "Attempt"),
            new Hook("HttpRequestImpl", "getUserHeaders", "Headers"),
            new Hook("Response", "<init>", "Response"));
    }
    public static class Hook implements TypeInstrumentation {
        private final String type, method, advice;
        Hook(String type, String method, String advice) { this.type=type;this.method=method;this.advice=advice; }
        public ElementMatcher<TypeDescription> typeMatcher() { return named("jdk.internal.net.http."+type); }
        public void transform(TypeTransformer transformer) {
            transformer.applyAdviceToMethod(method.equals("<init>") ? isConstructor() : named(method).and(takesArguments(type.equals("MultiExchange") ? 1 : 0)),
                JdkTransportAdvice.class.getName()+"$"+advice);
        }
    }
}
