package ai.mutuus.sample.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.mutuus.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DB 랩 페이지(/demo/db.html) 백엔드 — <b>실제 DB 에 연동되어 동작</b>하는 케이스 모음.
 * <ul>
 *   <li>CRUD(실 primary DB): {@code /crud} (POST/GET/PUT/DELETE)</li>
 *   <li>CRUD 오류: 중복 name→409, 미존재 id→404, 빈 name→400(@Valid), 트랜잭션 롤백→미저장</li>
 *   <li>읽기/쓰기 분리: {@code /rw/write}·{@code /rw/read}·{@code /rw/status}</li>
 *   <li>XA 성공/실패: {@code /xa/commit}·{@code /xa/rollback}·{@code /xa/status}·{@code /xa/reset}</li>
 * </ul>
 */
@RestController
@RequestMapping("/demo/db/lab")
public class DbLabController {

    private final DbLabSupport crud;
    private final DbDemoSupport db;
    private final DbMultiSupport multi;

    public DbLabController(DbLabSupport crud, DbDemoSupport db, DbMultiSupport multi) {
        this.crud = crud;
        this.db = db;
        this.multi = multi;
    }

    // ---------------- CRUD (실 DB) ----------------

    @PostMapping("/crud")
    public ResponseEntity<?> create(@Valid @RequestBody ItemRequest req) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(crud.create(req.name(), req.qty())));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("CONFLICT", "이미 존재하는 name 입니다(UNIQUE 제약): " + req.name()));
        }
    }

    @GetMapping("/crud")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(crud.list());
    }

    @GetMapping("/crud/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        Map<String, Object> row = crud.get(id);
        if (row == null) {
            return notFound(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(row));
    }

    @PutMapping("/crud/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @Valid @RequestBody ItemRequest req) {
        try {
            int rows = crud.update(id, req.name(), req.qty());
            if (rows == 0) {
                return notFound(id);
            }
            return ResponseEntity.ok(ApiResponse.ok(crud.get(id)));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("CONFLICT", "이미 존재하는 name 입니다(UNIQUE 제약): " + req.name()));
        }
    }

    @DeleteMapping("/crud/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        int rows = crud.delete(id);
        if (rows == 0) {
            return notFound(id);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("deletedId", id);
        res.put("deleted", true);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /** 트랜잭션 롤백: insert 후 예외 → 미저장(countByName=0). */
    @PostMapping("/crud/rollback")
    public ApiResponse<Map<String, Object>> rollback(@Valid @RequestBody ItemRequest req) {
        try {
            crud.insertThenFail(req.name(), req.qty()); // 프록시 경유 → @Transactional 적용
        } catch (RuntimeException expected) {
            // 기대된 예외 — 트랜잭션 롤백됨
        }
        int cnt = crud.countByName(req.name());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("rolledBack", cnt == 0);
        res.put("countByName", cnt);
        res.put("note", "insert 후 예외 발생 → @Transactional 롤백 → 실제 DB 에 저장되지 않음(countByName=0).");
        return ApiResponse.ok(res);
    }

    private ResponseEntity<?> notFound(long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "id=" + id + " 인 항목이 없습니다."));
    }

    // ---------------- 읽기/쓰기 분리 ----------------

    // 3개 DB(db1/db2/db3) 각 read/write — 프로퍼티 기반 라이브러리 라우팅(YAML만으로 구성).
    @PostMapping("/multidb/{db}/write")
    public ApiResponse<Map<String, Object>> multiWrite(@PathVariable("db") String dbName,
                                                       @Valid @RequestBody NameRequest req) {
        return ApiResponse.ok(multi.write(dbName, req.name()));
    }

    @GetMapping("/multidb/{db}/read")
    public ApiResponse<Map<String, Object>> multiRead(@PathVariable("db") String dbName) {
        return ApiResponse.ok(multi.read(dbName));
    }

    @GetMapping("/multidb/status")
    public ApiResponse<Map<String, Object>> multiStatus() {
        return ApiResponse.ok(multi.statusAll());
    }

    // ---------------- XA 성공/실패 ----------------

    @PostMapping("/xa/commit")
    public ApiResponse<Map<String, Object>> xaCommit(@Valid @RequestBody ValueRequest req) throws Exception {
        return ApiResponse.ok(db.xaCommit(req.value()));
    }

    @PostMapping("/xa/rollback")
    public ApiResponse<Map<String, Object>> xaRollback(@Valid @RequestBody ValueRequest req) throws Exception {
        return ApiResponse.ok(db.xaRollback(req.value()));
    }

    @GetMapping("/xa/status")
    public ApiResponse<Map<String, Object>> xaStatus() throws Exception {
        return ApiResponse.ok(db.xaCounts());
    }

    @PostMapping("/xa/reset")
    public ApiResponse<Map<String, Object>> xaReset() throws Exception {
        return ApiResponse.ok(db.xaReset());
    }

    // ---------------- 요청 DTO ----------------

    public record ItemRequest(@NotBlank String name, int qty) {
    }

    public record NameRequest(@NotBlank String name) {
    }

    public record ValueRequest(@NotBlank String value) {
    }
}
