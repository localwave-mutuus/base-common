package ai.mutuus.sample.logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ai.mutuus.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그 뷰어(/demo/logs.html)가 <b>1.5초마다 폴링</b>하는 tail 엔드포인트. 데모 케이스가 아니라 뷰어 인프라라
 * <b>DemoCaseController 에서 분리</b>했다. 이렇게 별도 패키지({@code ai.mutuus.sample.logs})에 두면
 * controller-entry/method-logging 을 패키지({@code ai.mutuus.sample.web/demo})로 타게팅해도 이 폴링
 * 컨트롤러는 자연히 제외되어, 뷰어가 자기 자신의 폴링 로그로 도배되지 않는다(access/payload 는 경로
 * {@code /demo/logs/} 제외로 함께 차단).
 */
@RestController
@RequestMapping("/demo/logs")
public class LogTailController {

    /** target/logs 의 "가장 최근" 로그 파일에서 after 이후 줄을 반환. 뷰어 페이지가 주기적으로 폴링한다. */
    @GetMapping("/tail")
    public ApiResponse<Map<String, Object>> logsTail(@RequestParam(defaultValue = "-1") long after)
            throws IOException {
        Path dir = Path.of("target", "logs");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dir", dir.toAbsolutePath().toString());

        Path latest = null;
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                latest = s.filter(p -> p.getFileName().toString().endsWith(".log"))
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .orElse(null);
            }
        }
        if (latest == null) {
            result.put("file", null);
            result.put("total", 0);
            result.put("from", 0);
            result.put("lines", List.of());
            result.put("hint", "로그 파일 없음 — 케이스를 호출해 로그를 생성하세요.");
            return ApiResponse.ok(result);
        }

        List<String> all = Files.readAllLines(latest);
        int total = all.size();
        // after<0(초기 로드) → 마지막 200줄만, 그 외 → after 이후
        int from = (after < 0) ? Math.max(0, total - 200) : (int) Math.min(after, total);
        result.put("file", latest.getFileName().toString());
        result.put("total", total);
        result.put("from", from);
        result.put("lines", all.subList(from, total));
        return ApiResponse.ok(result);
    }
}
