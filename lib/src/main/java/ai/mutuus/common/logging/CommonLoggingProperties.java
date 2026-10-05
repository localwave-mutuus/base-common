package ai.mutuus.common.logging;

import java.util.ArrayList;
import java.util.List;

import ai.mutuus.common.core.LogFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 액세스 로깅 설정. {@code mutuus.common.logging.*}.
 */
@ConfigurationProperties(prefix = "mutuus.common.logging")
public class CommonLoggingProperties {
    /** 프로젝트별 파일 굴림 정책. 기동 전 EnvironmentPostProcessor가 검증한다. */
    private File file = new File();
    public File getFile() { return file; }
    public void setFile(File value) { file = value; }

    public static class File {
        private String rollInterval = ai.mutuus.common.core.LogFilePolicy.DEFAULT_ROLL_INTERVAL;
        private String maxFileSize = ai.mutuus.common.core.LogFilePolicy.DEFAULT_MAX_FILE_SIZE;
        public String getRollInterval() { return rollInterval; }
        public void setRollInterval(String value) { rollInterval = value; }
        public String getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(String value) { maxFileSize = value; }
    }

    /** 액세스 로깅 전체 활성화 여부. */
    private boolean enabled = true;

    /**
     * 로그 필드 포맷. {@link LogFormat#DUAL}(기본) 이면 기존 필드 + ECS 필드를 함께 남긴다(전환기).
     * ECS 로 완전 이관 후 {@link LogFormat#ECS}, 되돌리려면 {@link LogFormat#LEGACY}.
     */
    private LogFormat format = LogFormat.DUAL;

    /** 배포 환경(local/dev/stage/prod). ECS {@code service.environment}/{@code data_stream.namespace} 의 근거. */
    private String environment;

    /** 실행 단위 번호: ECS task id 앞 8자 또는 VM instance id 끝 8자. LOCAL은 자동 생성. */
    private String serial;
    public String getSerial() { return serial; }
    public void setSerial(String value) { serial = value; }

    /** 컨테이너 /runtime/logs, VM /var/log/gs. 소유자가 배포 설정으로 지정한다. */
    private String directory;
    public String getDirectory() { return directory; }
    public void setDirectory(String value) { directory = value; }

    /** data stream 관련 설정. */
    private DataStream dataStream = new DataStream();

    /** 요청 수신 로그에 쿼리스트링 포함 여부. */
    private boolean includeQueryString = true;

    /** 호환 속성. 0.2.5 HTTP 짝은 /actuator 및 /actuator/**만 제외하며 추가 제외는 적용하지 않는다. */
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/actuator"));

    /** 느린 요청 경고 임계값(ms). 0이면 비활성. 초과 시 request.completed 가 WARN 으로 기록된다. */
    private long slowRequestThresholdMillis = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LogFormat getFormat() {
        return format;
    }

    public void setFormat(LogFormat format) {
        this.format = format;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public DataStream getDataStream() {
        return dataStream;
    }

    public void setDataStream(DataStream dataStream) {
        this.dataStream = dataStream;
    }

    public boolean isIncludeQueryString() {
        return includeQueryString;
    }

    public void setIncludeQueryString(boolean includeQueryString) {
        this.includeQueryString = includeQueryString;
    }

    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes;
    }

    public long getSlowRequestThresholdMillis() {
        return slowRequestThresholdMillis;
    }

    public void setSlowRequestThresholdMillis(long slowRequestThresholdMillis) {
        this.slowRequestThresholdMillis = slowRequestThresholdMillis;
    }

    /** data stream 설정. namespace 미지정 시 {@code environment} 를 사용한다(수집기/ingest 라우팅 근거). */
    public static class DataStream {

        /** ECS {@code data_stream.namespace}(prod/dev/local 등). 미지정 시 {@code environment} 로 대체. */
        private String namespace;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }
}
