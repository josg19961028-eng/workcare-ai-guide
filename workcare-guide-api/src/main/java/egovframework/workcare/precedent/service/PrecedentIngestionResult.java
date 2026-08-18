package egovframework.workcare.precedent.service;

/**
 * 공공데이터 판례 수집 작업의 처리 결과다.
 *
 * @param page         수집한 공공데이터 페이지
 * @param fetchedCount 공공데이터에서 조회한 판례 수
 * @param storedCount  Oracle MERGE가 처리한 판례 수
 * @param skippedCount 필수값이 없어 저장하지 않은 판례 수
 */
public record PrecedentIngestionResult(
        int page,
        int fetchedCount,
        int storedCount,
        int skippedCount
) {
}