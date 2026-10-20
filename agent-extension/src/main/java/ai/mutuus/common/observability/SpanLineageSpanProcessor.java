package ai.mutuus.common.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.management.*;

/** Actual SDK lineage, exposed across class loaders using only JDK types. No application SDK dependency. */
public final class SpanLineageSpanProcessor extends NotificationBroadcasterSupport implements SpanProcessor, DynamicMBean {
    public static final String NAME = "ai.mutuus.common:type=SpanLineage";
    private final Map<String, String> live = new HashMap<>();
    private final int maximum;
    private final Map<String, Map<String,Object>> clients = new HashMap<>();
    private final Map<String, String> expectedParents = new HashMap<>();
    private final ThreadLocal<java.util.List<String>> pending = ThreadLocal.withInitial(java.util.ArrayList::new);
    private final Map<String, Map<String,Object>> liveClients = new HashMap<>();
    private final Set<String> transportCompleted = new java.util.HashSet<>();
    private long dropped;
    private boolean registered;
    private boolean closed;
    private Set<String> allowedHosts = Set.of();
    private boolean transportEnabled;
    private long sequence;
    public synchronized void configureTransport(java.util.List<String> hosts) {
        allowedHosts=Set.copyOf(hosts.stream().map(h->h.toLowerCase(java.util.Locale.ROOT)).toList());transportEnabled=true;
    }
    public SpanLineageSpanProcessor(int maximum) {
        if (maximum < 1) throw new IllegalArgumentException("Positive lineage limit required");
        this.maximum = maximum;
    }
    public synchronized void register() {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(NAME));
            registered = true;
        } catch (JMException e) { throw new IllegalStateException("Lineage bridge registration failed"); }
    }
    @Override public synchronized void onStart(Context parentContext, ReadWriteSpan span) {
        if (closed) return;
        if (live.size() >= maximum) { if (dropped < Long.MAX_VALUE) dropped++; return; }
        var parent = Span.fromContext(parentContext).getSpanContext();
        var current = span.getSpanContext();
        if (span.getKind() == io.opentelemetry.api.trace.SpanKind.CLIENT) {
            Map<String,Object> fields = new HashMap<>();
            fields.put("trace.id", current.getTraceId()); fields.put("span.id", current.getSpanId());
            fields.put("trace.flags", Byte.toUnsignedInt(current.getTraceFlags().asByte()));
            if (parent.isValid() && parent.getTraceId().equals(current.getTraceId())) fields.put("parent.id", parent.getSpanId());
            var frozen = Map.copyOf(fields);
            liveClients.put(current.getTraceId() + current.getSpanId(), frozen);
            for (String token : pending.get()) {
                if (Objects.equals(expectedParents.get(token), parent.isValid() ? parent.getTraceId() + parent.getSpanId() : ""))
                    clients.putIfAbsent(token, frozen);
            }
        }
        live.put(current.getTraceId() + current.getSpanId(), parent.isValid()
                && parent.getTraceId().equals(current.getTraceId()) ? parent.getSpanId() : "");
    }
    @Override public synchronized void onEnd(ReadableSpan span) {
        var id = span.getSpanContext(); live.remove(id.getTraceId() + id.getSpanId()); liveClients.remove(id.getTraceId() + id.getSpanId());
    }
    @Override public boolean isStartRequired() { return true; }
    @Override public boolean isEndRequired() { return true; }
    @Override public synchronized CompletableResultCode shutdown() {
        closed = true; live.clear(); liveClients.clear(); clients.clear(); expectedParents.clear(); transportCompleted.clear(); pending.remove();
        if (registered) {
            try { ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(NAME)); }
            catch (JMException ignored) { return CompletableResultCode.ofFailure(); }
            registered = false;
        }
        return CompletableResultCode.ofSuccess();
    }
    @Override public synchronized Object invoke(String action, Object[] args, String[] signature) throws ReflectionException {
        if ("currentClientObservation".equals(action)) {
            var tokens=pending.get();
            if(tokens.isEmpty()){pending.remove();return "";}
            return tokens.getLast();
        }
        if ("attachTransportClient".equals(action) && args.length==3) {
            var snapshot=liveClients.get(args[1].toString()+args[2]);
            if(snapshot!=null && Objects.equals(expectedParents.get(args[0]),
                    snapshot.get("trace.id").toString()+Objects.toString(snapshot.get("parent.id"),"")))
                clients.putIfAbsent((String)args[0],snapshot);
            return null;
        }
        if ("approvedHost".equals(action)) return transportEnabled && args[0] instanceof String host && allowedHosts.contains(host.toLowerCase(java.util.Locale.ROOT));
        if ("transportComplete".equals(action) && args[0] instanceof Map<?,?> fields) {
            clients.forEach((token, snapshot) -> {
                if (Objects.equals(snapshot.get("trace.id"), fields.get("trace.id"))
                        && Objects.equals(snapshot.get("span.id"), fields.get("span.id"))) transportCompleted.add(token);
            });
            var notification=new Notification("ai.mutuus.common.transport.complete",NAME,++sequence);
            notification.setUserData(Map.copyOf(fields));sendNotification(notification);return null;
        }
        if ("beginClient".equals(action) && args != null && args.length == 2
                && args[0] instanceof String trace && args[1] instanceof String span) {
            if (closed || expectedParents.size() >= maximum) return "";
            String token = java.util.UUID.randomUUID().toString();
            String key = trace + span;
            expectedParents.put(token, key);
            if (liveClients.containsKey(key)) clients.put(token, liveClients.get(key));
            pending.get().add(token);
            return token;
        }
        if ("detachClient".equals(action) && args != null && args.length == 1 && args[0] instanceof String token) {
            pending.get().remove(token); if (pending.get().isEmpty()) pending.remove(); return null;
        }
        if ("finishClient".equals(action) && args != null && args.length == 1 && args[0] instanceof String token) {
            expectedParents.remove(token);
            var snapshot=clients.remove(token);
            boolean completed=transportCompleted.remove(token);
            if(snapshot==null)return null;
            var result=new HashMap<String,Object>(snapshot);
            result.put("_transport.completed",completed);
            return Map.copyOf(result);
        }
        if ("parentId".equals(action) && args != null && args.length == 2
                && args[0] instanceof String trace && args[1] instanceof String span)
            return live.get(trace + span);
        throw new ReflectionException(new NoSuchMethodException());
    }
    @Override public synchronized Object getAttribute(String name) throws AttributeNotFoundException {
        return switch (name) { case "Active" -> live.size(); case "Dropped" -> dropped; case "TransportEnabled" -> transportEnabled;
            default -> throw new AttributeNotFoundException(name); };
    }
    @Override public void setAttribute(Attribute value) throws AttributeNotFoundException { throw new AttributeNotFoundException(); }
    @Override public AttributeList getAttributes(String[] names) {
        var list = new AttributeList();
        for (String name : names) try { list.add(new Attribute(name, getAttribute(name))); } catch (AttributeNotFoundException ignored) { }
        return list;
    }
    @Override public AttributeList setAttributes(AttributeList values) { return new AttributeList(); }
    @Override public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(getClass().getName(), "SDK actual lineage",
            new MBeanAttributeInfo[]{new MBeanAttributeInfo("Active", "java.lang.Integer", "Live entries", true, false, false),
                new MBeanAttributeInfo("Dropped", "java.lang.Long", "Limit drops", true, false, false)}, null,
            new MBeanOperationInfo[]{new MBeanOperationInfo("parentId", "Actual parent",
                new MBeanParameterInfo[]{new MBeanParameterInfo("trace", "java.lang.String", "trace"),
                    new MBeanParameterInfo("span", "java.lang.String", "span")}, "java.lang.String", MBeanOperationInfo.INFO),
                new MBeanOperationInfo("beginClient", "Capture actual CLIENT during dispatch", new MBeanParameterInfo[]{
                    new MBeanParameterInfo("trace", "java.lang.String", "caller trace"), new MBeanParameterInfo("span", "java.lang.String", "caller span")}, "java.lang.String", MBeanOperationInfo.ACTION),
                new MBeanOperationInfo("detachClient", "Release dispatch thread", new MBeanParameterInfo[]{new MBeanParameterInfo("token", "java.lang.String", "token")}, "void", MBeanOperationInfo.ACTION),
                new MBeanOperationInfo("finishClient", "Remove and return immutable CLIENT snapshot", new MBeanParameterInfo[]{new MBeanParameterInfo("token", "java.lang.String", "token")}, "java.util.Map", MBeanOperationInfo.ACTION)}, null);
    }
}
