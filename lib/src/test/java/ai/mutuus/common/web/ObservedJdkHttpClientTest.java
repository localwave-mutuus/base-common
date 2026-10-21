package ai.mutuus.common.web;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.*;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.*;
class ObservedJdkHttpClientTest {
    @Test void syncAsyncAndCancelEmitOnceAndDoNotLogSecretsOrClaimCallerAsClient() throws Exception {
        var server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        var executor=Executors.newCachedThreadPool();server.setExecutor(executor);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        server.createContext("/",exchange->{
            try {
                if(exchange.getRequestURI().getPath().equals("/slow")) { entered.countDown();release.await(); }
                exchange.sendResponseHeaders(401,-1);
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });server.start();
        var log=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var completed=new CountDownLatch(3);
        var appender=new ListAppender<ILoggingEvent>() { @Override protected synchronized void append(ILoggingEvent event) { super.append(event); completed.countDown(); } }; appender.start();log.addAppender(appender);
        try(var client=new ObservedJdkHttpClient(HttpClient.newHttpClient())) {
            String root="http://localhost:"+server.getAddress().getPort();
            var request=HttpRequest.newBuilder(URI.create(root+"/private?token=SECRET")).build();
            assertThat(client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
            assertThat(client.sendAsync(request,HttpResponse.BodyHandlers.discarding()).get().statusCode()).isEqualTo(401);
            var future=client.sendAsync(HttpRequest.newBuilder(URI.create(root+"/slow")).build(),HttpResponse.BodyHandlers.discarding());
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();assertThat(future.cancel(true)).isTrue();
            release.countDown();
            assertThat(completed.await(5,TimeUnit.SECONDS)).isTrue();
            assertThat(appender.list).hasSize(3);
            for(var event:appender.list) {
                assertThat(event.getFormattedMessage()).isEqualTo("http.client.completed");
                assertThat(event.getKeyValuePairs().toString()).contains("failure").doesNotContain("SECRET","private");
            }
            assertThat(appender.list.getLast().getKeyValuePairs().toString()).contains("HTTP_CANCELLED").doesNotContain("status_code");
        } finally { release.countDown();server.stop(0);executor.shutdownNow();log.detachAppender(appender);appender.stop(); }
    }
    @Test void nonTransportCompletionLogsOnlyDestinationHost() throws Exception {
        assertThat(ObservedJdkHttpClient.transportLoggingEnabled()).isFalse();
        var delegate=org.mockito.Mockito.mock(HttpClient.class);
        var request=HttpRequest.newBuilder(URI.create("https://user:password@destination.example:8443/private-path?token=SECRET_QUERY")).build();
        var handler=HttpResponse.BodyHandlers.discarding();
        var response=org.mockito.Mockito.mock(HttpResponse.class);
        org.mockito.Mockito.when(response.statusCode()).thenReturn(204);
        org.mockito.Mockito.doReturn(response).when(delegate).send(request,handler);
        org.mockito.Mockito.doReturn(CompletableFuture.completedFuture(response)).when(delegate).sendAsync(request,handler);
        var logger=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var client=new ObservedJdkHttpClient(delegate);
            assertThat(client.send(request,handler).statusCode()).isEqualTo(204);
            assertThat(client.sendAsync(request,handler).get().statusCode()).isEqualTo(204);
            assertThat(appender.list).hasSize(2);
            for(var event:appender.list) {
                assertThat(event.getKeyValuePairs()).filteredOn(pair->pair.key.equals("server.address"))
                    .extracting(pair->pair.value).containsExactly("destination.example");
                assertThat(event.getKeyValuePairs()).filteredOn(pair->pair.key.equals("url.path"))
                    .extracting(pair->pair.value).containsExactly("/{unmatched}");
                assertThat(event.getFormattedMessage()+event.getKeyValuePairs())
                    .doesNotContain("https://","user","password","8443","private-path","token","SECRET_QUERY");
            }
        } finally {logger.detachAppender(appender);appender.stop();}
    }
    @Test void writerFailureDoesNotChangeApplicationOutcomeOrRetryCompletion() {
        var scope=new HttpCompletionScope();long before=HttpCompletionScope.writeFailureCount();
        assertThat(scope.finish(200,null,done->{throw new IllegalStateException("private");})).isTrue();
        assertThat(scope.finish(500,"HTTP_500",done->{throw new AssertionError();})).isFalse();
        assertThat(HttpCompletionScope.writeFailureCount()).isEqualTo(before+1);
    }
    @Test void timeoutAndCommunicationFailuresHaveOneSafeCompletionEach() throws Exception {
        var delegate=org.mockito.Mockito.mock(HttpClient.class);
        var request=HttpRequest.newBuilder(URI.create("http://localhost/secret?token=PRIVATE")).build();
        var handler=HttpResponse.BodyHandlers.discarding();
        var logger=(Logger)LoggerFactory.getLogger(HttpClientLoggingInterceptor.LOGGER_NAME);
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var client=new ObservedJdkHttpClient(delegate);
            org.mockito.Mockito.doThrow(new java.io.IOException("PRIVATE")).when(delegate).send(request,handler);
            assertThatThrownBy(()->client.send(request,handler)).isInstanceOf(java.io.IOException.class);
            org.mockito.Mockito.doReturn(CompletableFuture.failedFuture(new HttpTimeoutException("PRIVATE"))).when(delegate).sendAsync(request,handler);
            assertThatThrownBy(()->client.sendAsync(request,handler).join()).hasCauseInstanceOf(HttpTimeoutException.class);
            org.mockito.Mockito.doThrow(new IllegalStateException("PRIVATE")).when(delegate).sendAsync(request,handler);
            assertThatThrownBy(()->client.sendAsync(request,handler)).isInstanceOf(IllegalStateException.class);
            assertThat(appender.list).hasSize(3);
            assertThat(appender.list.get(0).getKeyValuePairs().toString()).contains("HTTP_IO_ERROR");
            assertThat(appender.list.get(1).getKeyValuePairs().toString()).contains("HTTP_TIMEOUT");
            assertThat(appender.list.get(2).getKeyValuePairs().toString()).contains("HTTP_IO_ERROR");
            for(var event:appender.list)assertThat(event.getKeyValuePairs().toString()).doesNotContain("PRIVATE","status_code");
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
