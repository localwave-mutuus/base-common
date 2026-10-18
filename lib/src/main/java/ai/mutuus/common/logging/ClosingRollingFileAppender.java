package ai.mutuus.common.logging;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.core.rolling.RollingFileAppender;

/** 조용한 서비스도 주기적으로 닫힌 파일을 공개하고, 정상 종료 시 마지막 파일까지 닫는다. */
public class ClosingRollingFileAppender<E> extends RollingFileAppender<E> {
    private final Object lifecycleLock = new Object();
    private long forceRollSeconds = 300;
    private ScheduledFuture<?> forceRollTask;
    private Object timerGeneration;

    public void setForceRollSeconds(long seconds) {
        if (seconds < 0) throw new IllegalArgumentException("forceRollSeconds must be nonnegative");
        synchronized (lifecycleLock) {
            if (isStarted()) throw new IllegalStateException("forceRollSeconds must be set before start");
            forceRollSeconds = seconds;
        }
    }

    public long getForceRollSeconds() {
        synchronized (lifecycleLock) { return forceRollSeconds; }
    }

    @Override public void start() {
        synchronized (lifecycleLock) {
            if (isStarted()) return;
            // 쓰기 오류로 상위 appender만 중지된 뒤 재시작해도 이전 타이머를 남기지 않는다.
            timerGeneration = null;
            if (forceRollTask != null) {
                forceRollTask.cancel(false);
                forceRollTask = null;
            }
            super.start();
            if (!isStarted() || forceRollSeconds == 0) return;
            Object generation = new Object();
            timerGeneration = generation;
            try {
                // 공유 실행기는 Context가 소유한다. 이 appender는 자기 작업만 취소한다.
                forceRollTask = getContext().getScheduledExecutorService().scheduleWithFixedDelay(
                        () -> forceRoll(generation), forceRollSeconds, forceRollSeconds, TimeUnit.SECONDS);
            } catch (RuntimeException ex) {
                addError("강제 로그 회전 타이머 시작 실패", ex);
                stop();
            }
        }
    }

    private void forceRoll(Object generation) {
        synchronized (lifecycleLock) {
            if (!isStarted() || timerGeneration != generation) return;
            try {
                streamWriteLock.lock();
                try {
                    if (getOutputStream() != null) getOutputStream().flush();
                    String active = getFile();
                    if (active != null && new File(active).length() > 0) rollover();
                } finally {
                    streamWriteLock.unlock();
                }
            } catch (IOException ex) {
                addError("강제 로그 회전 전 flush 실패; 다음 주기에 재시도", ex);
            } catch (RuntimeException ex) {
                addError("강제 로그 회전 실패; 다음 주기에 재시도", ex);
            }
        }
    }

    @Override protected void subAppend(E event) {
        synchronized (lifecycleLock) {
            // 상위 구현의 trigger 검사와 실제 쓰기를 한 경계로 묶어 타이머와 직렬화한다.
            if (isStarted()) super.subAppend(event);
        }
    }

    @Override public void rollover() {
        synchronized (lifecycleLock) {
            if (!isStarted()) return;
            // 잠금 순서: lifecycle -> 상위 trigger(쓰기 경로만) -> stream.
            // super.rollover도 같은 재진입 가능한 stream 잠금을 사용한다.
            streamWriteLock.lock();
            try {
                if (getOutputStream() != null) getOutputStream().flush();
                // 빈 파일도 policy의 시간 버킷은 갱신하되, policy가 빈 archive 공개를 막는다.
                super.rollover();
            } catch (IOException ex) {
                addError("로그 회전 전 flush 실패; 활성 파일 보존", ex);
            } finally {
                streamWriteLock.unlock();
            }
        }
    }

    @Override public void stop() {
        synchronized (lifecycleLock) {
            timerGeneration = null;
            if (forceRollTask != null) {
                // 진행 중인 압축을 interrupt하지 않는다. 같은 잠금으로 완료를 기다린다.
                forceRollTask.cancel(false);
                forceRollTask = null;
            }
            try {
                // AsyncAppender가 잔여 큐를 비운 뒤 호출하며, 빈 파일은 공개하지 않는다.
                if (isStarted()) rollover();
            } finally {
                super.stop();
            }
        }
    }
}
