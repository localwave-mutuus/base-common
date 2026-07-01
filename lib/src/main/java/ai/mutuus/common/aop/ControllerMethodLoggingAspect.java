package ai.mutuus.common.aop;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.PatternMatchUtils;

/**
 * 컨트롤러 메서드 인자/리턴 로깅 Aspect.
 * <p>{@code @RestController} 빈의 모든 메서드를 {@code @Around} 로 감싸, <b>진입 시 역직렬화된 인자</b>,
 * <b>정상 종료 시 리턴 객체</b>를 {@link MethodLoggingWriter} 로 출력한다. 필터/인터셉터가 못 보는
 * "메서드 실제 인자/리턴"이 이 계층에서 관측된다. 대상 축소(패키지/메서드명)는 {@link MethodLoggingProperties}.
 * <p>예외로 빠지면 종료 로그는 남기지 않는다(오류는 GlobalExceptionHandler 가 error.* 로 기록).
 */
@Aspect
public class ControllerMethodLoggingAspect {

    private final MethodLoggingWriter writer;
    private final MethodLoggingProperties props;

    public ControllerMethodLoggingAspect(MethodLoggingWriter writer, MethodLoggingProperties props) {
        this.writer = writer;
        this.props = props;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logMethod(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String type = signature.getDeclaringType().getSimpleName();
        String method = signature.getName();

        // 대상이 아니면 로깅 없이 원 메서드만 실행
        if (!matches(signature)) {
            return pjp.proceed();
        }
        writer.onEnter(type, method, pjp.getArgs()); // 진입: 인자
        Object result = pjp.proceed();               // 원 메서드 실행
        writer.onExit(type, method, result);         // 정상 종료: 리턴
        return result;
    }

    private boolean matches(MethodSignature signature) {
        // 패키지/메서드명 두 축(AND). 비어 있는 축은 제약하지 않는다.
        List<String> packages = props.getPackages();
        if (!packages.isEmpty()) {
            String typeName = signature.getDeclaringType().getName();
            if (packages.stream().noneMatch(typeName::startsWith)) {
                return false;
            }
        }
        List<String> names = props.getMethodNames();
        if (!names.isEmpty()) {
            String methodName = signature.getName();
            if (names.stream().noneMatch(p -> PatternMatchUtils.simpleMatch(p, methodName))) {
                return false;
            }
        }
        return true;
    }
}
