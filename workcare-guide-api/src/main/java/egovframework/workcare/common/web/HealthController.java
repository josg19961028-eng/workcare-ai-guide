package egovframework.workcare.common.web;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import egovframework.workcare.common.web.dto.HealthResponse;

/**
 * WorkCare Guide 백엔드 서버가 정상적으로 동작하는지 확인하는 Controller다.
 *
 * <p>브라우저, 프론트엔드, 모니터링 시스템이 이 API를 호출하여
 * 서버가 HTTP 요청을 정상적으로 처리하는지 확인할 수 있다.</p>
 *
 * <p>상태 확인 API에는 비밀번호, DB 접속 정보, 공공데이터 인증키 같은
 * 내부 설정을 절대로 포함하면 안 된다. 외부 사용자가 인증 없이
 * 호출할 가능성이 있기 때문이다.</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 서버의 기본 상태를 조회한다.
     *
     * <p>이 메서드는 데이터를 변경하지 않는 조회 기능이므로
     * HTTP GET 방식을 사용한다.</p>
     *
     * @return HTTP 200 상태와 서버 상태 정보
     */
    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {

        /*
         * 응답 객체를 생성한다.
         *
         * Instant.now()는 현재 시각을 UTC 기준 시점으로 반환한다.
         * 서버가 서로 다른 국가나 시간대에 배포되어도 동일한 기준으로
         * 시간을 비교할 수 있다.
         */
        HealthResponse response = new HealthResponse(
                "UP",
                "workcare-guide-api",
                Instant.now()
        );

        /*
         * ResponseEntity.ok(...)는 HTTP 상태 코드 200 OK와 함께
         * response 객체를 JSON으로 변환하여 클라이언트에 반환한다.
         */
        return ResponseEntity.ok(response);
    }
}