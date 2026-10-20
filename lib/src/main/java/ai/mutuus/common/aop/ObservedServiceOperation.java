package ai.mutuus.common.aop;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface ObservedServiceOperation { }
