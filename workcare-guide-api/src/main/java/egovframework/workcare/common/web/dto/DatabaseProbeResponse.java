package egovframework.workcare.common.web.dto;

/**
 * 개발환경 DB 연결 확인 API의 응답 데이터다.
 *
 * <p>DB 비밀번호, JDBC URL, 내부 IP 주소와 같은 민감한 설정은
 * 응답에 포함하지 않는다. 진단 API에서도 필요한 최소 정보만
 * 외부에 공개해야 한다.</p>
 *
 * @param status      DB 연결 상태
 * @param database    연결 대상 DB 종류
 * @param queryResult DB가 반환한 확인값
 */
public record DatabaseProbeResponse(
        String status,
        String database,
        int queryResult
) {
}