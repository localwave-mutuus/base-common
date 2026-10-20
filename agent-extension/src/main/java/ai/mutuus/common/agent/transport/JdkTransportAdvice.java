package ai.mutuus.common.agent.transport;

import java.net.http.*;
import java.util.concurrent.CompletableFuture;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AssignReturned;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

public final class JdkTransportAdvice {
    public static class Call {
        @Advice.OnMethodExit(inline=false)
        public static void exit(@Advice.This Object multi) { JdkTransportState.capture(multi); }
    }
    public static class Cleanup {
        @Advice.OnMethodExit(inline=false,onThrowable=Throwable.class)
        public static void exit(@Advice.This Object multi,@Advice.Return CompletableFuture<?> future,@Advice.Thrown Throwable error) {
            if(error!=null)JdkTransportState.release(multi,error);
            else future.whenComplete((response,failure)->JdkTransportState.release(multi,failure));
        }
    }
    public static class Attempt {
        @Advice.OnMethodEnter(inline=false)
        public static JdkTransportState.Attempt enter(@Advice.This Object exchange,
                @Advice.FieldValue("multi") Object multi,
                @Advice.FieldValue(value="request", typing=Assigner.Typing.DYNAMIC) HttpRequest request) {
            return JdkTransportState.begin(exchange,multi,request);
        }
        @AssignReturned.ToReturned
        @Advice.OnMethodExit(inline=false,onThrowable=Throwable.class)
        public static CompletableFuture<?> exit(@Advice.Enter JdkTransportState.Attempt attempt,
                @Advice.Return CompletableFuture<?> future, @Advice.Thrown Throwable failure) {
            if(failure!=null) { attempt.finish(failure); return future; }
            return future.whenComplete((response,error)->attempt.finish(error));
        }
    }
    public static class Headers {
        @AssignReturned.ToReturned
        @Advice.OnMethodExit(inline=false)
        public static HttpHeaders exit(@Advice.This HttpRequest request,@Advice.Return HttpHeaders headers) {
            return JdkTransportState.headers(request,headers);
        }
    }
    public static class Response {
        @Advice.OnMethodExit(inline=false)
        public static void exit(@Advice.FieldValue("exchange") Object exchange,@Advice.FieldValue("statusCode") int status) {
            JdkTransportState.status(exchange,status);
        }
    }
}
