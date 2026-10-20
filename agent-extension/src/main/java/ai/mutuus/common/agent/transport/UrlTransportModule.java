package ai.mutuus.common.agent.transport;

import io.opentelemetry.javaagent.extension.instrumentation.*;
import java.util.List;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import static net.bytebuddy.matcher.ElementMatchers.*;

/** Opt-in pinned HttpURLConnection transport, including Spring SimpleClientHttpRequestFactory. */
public final class UrlTransportModule extends InstrumentationModule {
    public UrlTransportModule(){super("mutuus-url-transport");}
    @Override public boolean defaultEnabled(){return false;}
    @Override public boolean isHelperClass(String name){
        return name.startsWith("ai.mutuus.common.agent.transport.UrlTransportState");
    }
    @Override public List<String> getAdditionalHelperClassNames(){
        return List.of(UrlTransportState.class.getName(),UrlTransportState.Attempt.class.getName());
    }
    @Override public List<TypeInstrumentation> typeInstrumentations(){
        return List.of(new TypeInstrumentation(){
            public ElementMatcher<TypeDescription> typeMatcher(){return named("sun.net.www.http.HttpClient");}
            public void transform(TypeTransformer transformer){
                transformer.applyAdviceToMethod(named("writeRequests").and(takesArguments(2)),UrlTransportAdvice.Write.class.getName());
                transformer.applyAdviceToMethod(named("parseHTTPHeader"),UrlTransportAdvice.Response.class.getName());
            }
        });
    }
}
