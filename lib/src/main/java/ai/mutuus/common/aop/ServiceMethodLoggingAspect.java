package ai.mutuus.common.aop;
import java.lang.reflect.*;
import java.util.concurrent.CompletionStage;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.PatternMatchUtils;
@Aspect
public class ServiceMethodLoggingAspect {
 private final ServiceMethodLoggingProperties props; private final ServiceMethodLoggingWriter writer;
 private ai.mutuus.common.payload.SafePayloadPolicy policy;
 public ServiceMethodLoggingAspect(ServiceMethodLoggingProperties p,ServiceMethodLoggingWriter w,ai.mutuus.common.payload.SafePayloadPolicy policy){this(p,w);this.policy=policy;}
 public ServiceMethodLoggingAspect(ServiceMethodLoggingProperties props,ServiceMethodLoggingWriter writer){this.props=props;this.writer=writer;}
 public boolean matches(Method m){
  String type=m.getDeclaringClass().getName(),name=m.getName();
  boolean excluded=type.matches("(?i).*(?:\\.|\\$)(?:security|repository|entity|dto|crypto|token)(?:\\.|\\$|$).*" )
   || name.matches("(?:get|set|is)[A-Z].*")
   || org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(m.getDeclaringClass(),org.springframework.stereotype.Repository.class)
   || org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(m.getDeclaringClass(),org.springframework.stereotype.Controller.class);
  return !excluded && props.isEnabled() && Modifier.isPublic(m.getModifiers())
   && props.getIncludePackages().stream().anyMatch(p->!p.isBlank() && (type.startsWith(p+".") || type.equals(p)))
   && props.getIncludeMethods().stream().anyMatch(p->PatternMatchUtils.simpleMatch(p,name))
   && props.getExcludeMethods().stream().noneMatch(p->PatternMatchUtils.simpleMatch(p,name))
   && (!props.isRequireAnnotation() || m.isAnnotationPresent(ObservedServiceOperation.class));
 }
 private static boolean reactive(Class<?> type){
  if(type.getName().equals("org.reactivestreams.Publisher") || type.getName().equals("java.util.concurrent.Flow$Publisher"))return true;
  for(Class<?> i:type.getInterfaces())if(reactive(i))return true;
  return type.getSuperclass()!=null && reactive(type.getSuperclass());
 }
 @Around("execution(public * *(..)) && (@within(org.springframework.stereotype.Service) || @annotation(ai.mutuus.common.aop.ObservedServiceOperation)) && !within(ai.mutuus.common.aop..*)")
 public Object observe(ProceedingJoinPoint pjp)throws Throwable{
  Method m=AopUtils.getMostSpecificMethod(((MethodSignature)pjp.getSignature()).getMethod(),pjp.getTarget().getClass());
  if(!matches(m))return pjp.proceed();
  if(reactive(m.getReturnType()))throw new IllegalArgumentException("Reactive service observation unsupported");
  var invocation=ServiceInvocationScope.open(m.getDeclaringClass().getName(),m.getName(),writer);
  try(var ignored=invocation.makeCurrent()){
   if(props.isPayloadEnabled() && policy!=null){
    var args=pjp.getArgs();
    ai.mutuus.common.payload.ApprovedInput input=args!=null && args.length==1 && args[0] instanceof ai.mutuus.common.payload.ApprovedInput approved?approved:null;
    payload(invocation,m,ai.mutuus.common.payload.HttpPayloadRule.Direction.INPUT,input,false);
   }
   Object result=pjp.proceed();
   if(result instanceof CompletionStage<?> stage)stage.whenComplete((value,error)->{try(var completion=invocation.makeCurrent()){
    if(props.isPayloadEnabled() && policy!=null)payload(invocation,m,ai.mutuus.common.payload.HttpPayloadRule.Direction.OUTPUT,value instanceof ai.mutuus.common.payload.ApprovedInput a?a:null,error!=null);
    if(error==null)invocation.success();else invocation.fail(error);
   }});
   else {
    if(props.isPayloadEnabled() && policy!=null)payload(invocation,m,ai.mutuus.common.payload.HttpPayloadRule.Direction.OUTPUT,result instanceof ai.mutuus.common.payload.ApprovedInput a?a:null,false);
    invocation.success();
   }
   return result;
  }catch(Throwable ex){
   if(props.isPayloadEnabled() && policy!=null)try(var failure=invocation.makeCurrent()){payload(invocation,m,ai.mutuus.common.payload.HttpPayloadRule.Direction.OUTPUT,null,true);}
   invocation.fail(ex);throw ex;
  }
 }
 private void payload(ServiceInvocationScope invocation,Method m,ai.mutuus.common.payload.HttpPayloadRule.Direction direction,ai.mutuus.common.payload.ApprovedInput input,boolean failure){
  var result=input==null?ai.mutuus.common.payload.SafePayloadPolicy.Result.omitted("POLICY_DENIED"):
   policy.service(new ai.mutuus.common.payload.ServicePayloadRule(m.getDeclaringClass().getName(),m.getName(),direction,"registered"),input,failure);
  new ai.mutuus.common.payload.SafePayloadEventWriter().write(result);
 }
}
