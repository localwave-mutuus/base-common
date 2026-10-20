package ai.mutuus.common.agent;
import ai.mutuus.common.observability.TelemetryContextSnapshot;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.*;
import io.opentelemetry.sdk.autoconfigure.spi.*;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class LineageInstallationTest {
    @Test void providerInstallsProcessorInSdkAndDisabledSettingDoesNotInstallIt() {
        for(boolean enabled:new boolean[]{false,true}) {
            var configuration=mock(ConfigProperties.class);
            when(configuration.getBoolean("mutuus.common.observability.lineage.enabled",true)).thenReturn(enabled);
            when(configuration.getInt("mutuus.common.observability.lineage.max-active-spans",100000)).thenReturn(10);
            var customizer=mock(AutoConfigurationCustomizer.class);
            doAnswer(call->{
                BiFunction<SdkTracerProviderBuilder,ConfigProperties,SdkTracerProviderBuilder> install=call.getArgument(0);
                try(var sdk=install.apply(SdkTracerProvider.builder(),configuration).build()) {
                    var parent=SpanContext.createFromRemoteParent("1234567890abcdef1234567890abcdef","abcdef1234567890",TraceFlags.getSampled(),TraceState.getDefault());
                    var span=sdk.get("fixture").spanBuilder("via-spi").setParent(Context.root().with(Span.wrap(parent))).startSpan();
                    try(var ignored=span.makeCurrent()) {
                        if(enabled)assertThat(TelemetryContextSnapshot.capture().fields()).containsEntry("parent.id",parent.getSpanId());
                        else assertThat(TelemetryContextSnapshot.capture().fields()).doesNotContainKey("parent.id");
                    } finally {span.end();}
                }
                return customizer;
            }).when(customizer).addTracerProviderCustomizer(any());
            new LineageAutoConfigurationCustomizerProvider().customize(customizer);
            verify(customizer).addTracerProviderCustomizer(any());
        }
    }
}
