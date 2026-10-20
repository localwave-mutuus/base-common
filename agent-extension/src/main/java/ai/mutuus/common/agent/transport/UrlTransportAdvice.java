package ai.mutuus.common.agent.transport;
import java.net.URL;
import net.bytebuddy.asm.Advice;
public final class UrlTransportAdvice {
    public static class Write {
        @Advice.OnMethodEnter(inline=false)
        public static UrlTransportState.Attempt enter(@Advice.This Object client,@Advice.FieldValue("url") URL url,@Advice.Argument(0) Object headers){
            return UrlTransportState.begin(client,url,headers);
        }
        @Advice.OnMethodExit(inline=false,onThrowable=Throwable.class)
        public static void exit(@Advice.This Object client,@Advice.Enter UrlTransportState.Attempt attempt,@Advice.Argument(0) Object headers,@Advice.Thrown Throwable error){
            if(error!=null)UrlTransportState.finish(client,attempt,headers,error);
        }
    }
    public static class Response {
        @Advice.OnMethodEnter(inline=false)
        public static UrlTransportState.Attempt enter(@Advice.This Object client){return UrlTransportState.current(client);}
        @Advice.OnMethodExit(inline=false,onThrowable=Throwable.class)
        public static void exit(@Advice.This Object client,@Advice.Enter UrlTransportState.Attempt attempt,@Advice.Argument(0) Object headers,@Advice.Thrown Throwable error){
            UrlTransportState.finish(client,attempt,headers,error);
        }
    }
}
