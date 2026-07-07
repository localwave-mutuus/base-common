package ai.mutuus.common.event;

import java.util.List;

import ai.mutuus.common.core.EcsFields;
import ai.mutuus.common.core.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * 도메인 이벤트 발행/소비를 로깅해 <b>비동기·이벤트 흐름</b>을 관측한다.
 * <p>발행({@code domain_event.published})은 {@link ApplicationEventPublisherAdapter} 가, 소비
 * ({@code domain_event.consumed})는 {@link DomainEventLoggingListener} 가 호출한다. 둘 다
 * {@code mutuus.domain_event} dataset. 같은 요청에서 비롯된 이벤트는 {@code trace.id}(MDC) 로
 * access·error 로그와 묶인다(in-process 동기 전달이면 발행/소비가 같은 스레드라 MDC 가 유지된다).
 * <p><b>주의</b>: in-process 소비 로그는 "리스너로 <b>전달</b>됐다"는 표시다 — 등록된 리스너가 없어도
 * Spring 이 전달을 시도하면 로깅 리스너가 받아 소비로 기록될 수 있다. 로거 이름 {@code ai.mutuus.common.event}.
 */
public class DomainEventLogger {

    public static final String LOGGER_NAME = "ai.mutuus.common.event";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    private static final List<String> TYPE_INFO = List.of("info");

    private final LogFormat format;

    public DomainEventLogger() {
        this(LogFormat.DUAL);
    }

    public DomainEventLogger(LogFormat format) {
        this.format = format == null ? LogFormat.DUAL : format;
    }

    /** 이벤트 발행(publish) 지점. */
    public void published(DomainEvent<?> event) {
        write("domain_event.published", event);
    }

    /** 이벤트 소비(consume/delivery) 지점. */
    public void consumed(DomainEvent<?> event) {
        write("domain_event.consumed", event);
    }

    private void write(String action, DomainEvent<?> event) {
        LoggingEventBuilder ev = log.atInfo();
        if (format.legacy()) {
            ev.addKeyValue("event", action)
                    .addKeyValue("eventId", event.eventId())
                    .addKeyValue("eventType", event.type());
        }
        if (format.ecs()) {
            ev.addKeyValue(EcsFields.EVENT_DATASET, EcsFields.DATASET_DOMAIN_EVENT)
                    .addKeyValue(EcsFields.DATA_STREAM_DATASET, EcsFields.DATASET_DOMAIN_EVENT)
                    .addKeyValue(EcsFields.EVENT_ACTION, action)
                    .addKeyValue(EcsFields.EVENT_TYPE, TYPE_INFO)
                    .addKeyValue(EcsFields.MUTUUS_DOMAIN_EVENT_ID, event.eventId())
                    .addKeyValue(EcsFields.MUTUUS_DOMAIN_EVENT_TYPE, event.type());
        }
        ev.log("domain event " + action);
    }
}
