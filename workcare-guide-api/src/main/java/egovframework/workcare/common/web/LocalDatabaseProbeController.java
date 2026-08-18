package egovframework.workcare.common.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.common.infrastructure.DatabaseProbeMapper;
import egovframework.workcare.common.web.dto.DatabaseProbeResponse;

/**
 * 개발환경에서 Oracle DB 연결 상태를 확인하는 Controller다.
 *
 * <p>이 API는 운영 업무 기능이 아니라 개발환경 진단 기능이다.
 * 따라서 local 프로필에서만 등록되도록 제한한다.</p>
 *
 * <p>운영환경에 DB 진단 API가 그대로 노출되면 공격자가
 * 내부 시스템 구성 정보를 수집하는 데 이용할 수 있으므로
 * 운영 프로필에서는 생성되지 않아야 한다.</p>
 */
@Profile("local")
@RestController
@RequestMapping("/api/local/database/probe")
public class LocalDatabaseProbeController {

    private final DatabaseProbeMapper databaseProbeMapper;

    /**
     * 생성자 주입을 통해 MyBatis Mapper를 전달받는다.
     *
     * @param databaseProbeMapper DB 연결 확인용 Mapper
     */
    public LocalDatabaseProbeController(
            DatabaseProbeMapper databaseProbeMapper
    ) {
        this.databaseProbeMapper = databaseProbeMapper;
    }

    /**
     * Oracle에 실제 SQL을 실행하여 연결 상태를 확인한다.
     *
     * @return DB 연결 확인 결과
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseProbeResponse> probeDatabase() {

        /*
         * 이 코드가 실행되는 순간 HikariCP가 Oracle 연결을 확보하고
         * MyBatis가 DatabaseProbeMapper.xml의 SQL을 실행한다.
         */
        int queryResult =
                databaseProbeMapper.selectConnectionCheck();

        /*
         * Oracle에서 예상한 값 1이 반환되었는지 확인한다.
         */
        String status = queryResult == 1 ? "UP" : "DOWN";

        /*
         * 보안상 DB 계정명, 비밀번호, JDBC 주소는 반환하지 않는다.
         */
        DatabaseProbeResponse response =
                new DatabaseProbeResponse(
                        status,
                        "Oracle",
                        queryResult
                );

        return ResponseEntity.ok(response);
    }
}