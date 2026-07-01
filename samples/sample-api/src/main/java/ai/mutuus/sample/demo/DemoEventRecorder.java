package ai.mutuus.sample.demo;

import ai.mutuus.common.event.DomainEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 이벤트 데모용 in-process 리스너. 라이브러리 {@code EventPublisher} 가 발행한 {@link DomainEvent} 봉투를
 * {@code @EventListener} 로 받아 마지막 수신분을 보관한다(데모 엔드포인트가 수신 여부/봉투를 되돌려 확인).
 */
@Component
public class DemoEventRecorder {

    private volatile DomainEvent<?> last;

    @EventListener
    public void onDomainEvent(DomainEvent<?> event) {
        this.last = event;
    }

    public DomainEvent<?> last() {
        return last;
    }
}
