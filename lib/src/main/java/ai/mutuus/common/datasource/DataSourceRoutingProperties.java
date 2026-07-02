package ai.mutuus.common.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 읽기/쓰기 라우팅 설정. {@code mutuus.common.datasource.routing.*}. <b>기본 OFF(opt-in)</b>.
 * <p>켜면 소비 서비스가 제공한 {@code writeDataSource}/{@code readDataSource} 빈으로 라이브러리가
 * {@code @Primary} 라우팅 DataSource 를 조립한다({@link CommonDataSourceRoutingAutoConfiguration}).
 */
@ConfigurationProperties(prefix = "mutuus.common.datasource.routing")
public class DataSourceRoutingProperties {

    /** 읽기/쓰기 라우팅 활성화 여부(기본 false). */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
