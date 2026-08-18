package egovframework.workcare.precedent.web.dto;

/**
 * 판례 수집 API가 반환하는 응답 데이터다.
 *
 * @param page         수집 페이지
 * @param fetchedCount 공공데이터 조회 건수
 * @param storedCount  Oracle 저장 처리 건수
 * @param skippedCount 저장 제외 건수
 */
public record PrecedentIngestionResponse(
        int page,
        int fetchedCount,
        int storedCount,
        int skippedCount
) {
}