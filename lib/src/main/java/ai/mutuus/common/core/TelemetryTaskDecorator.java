package ai.mutuus.common.core;
/** Explicit opt-in only: captures immutable lineage and OTel Context; never MDC or a new span. */
public final class TelemetryTaskDecorator implements org.springframework.core.task.TaskDecorator {
 @Override public Runnable decorate(Runnable task){return ai.mutuus.common.observability.TelemetryContextSnapshot.capture().wrap(task);}
}
