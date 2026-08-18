package egovframework.workcare.common.web.dto;

import java.time.Instant;

/**
 * 서버 상태 확인 API가 반환하는 응답 데이터다.
 *
 * <p>Controller에서 Map을 바로 반환할 수도 있지만,
 * 별도의 응답 타입을 선언하면 API가 반환하는 필드와 자료형을
 * 코드에서 명확하게 관리할 수 있다.</p>
 *
 * <p>record는 Java 16부터 정식으로 제공되는 불변 데이터 객체다.
 * 각 필드의 getter, 생성자, equals, hashCode, toString을
 * Java 컴파일러가 자동으로 만들어준다.</p>
 *
 * @param status    서버의 현재 상태
 * @param service   상태를 확인한 서비스 이름
 * @param checkedAt 상태 확인 시각
 */
public record HealthResponse(
        String status,
        String service,
        Instant checkedAt
) {
}