package egovframework.workcare.precedent.web.dto;

import java.util.List;

/**
 * 의미 기반 판례 검색 응답이다.
 *
 * @param resultCount 검색 결과 수
 * @param disclaimer  법률 판단이 아니라는 안내
 * @param results     관련 판례 목록
 */
public record SemanticPrecedentSearchResponse(
        int resultCount,
        String disclaimer,
        List<Result> results
) {

    /**
     * Vue에 반환할 관련 판례 한 건이다.
     */
    public record Result(
            String caseNumber,
            String courtName,
            String resultType,
            String caseType,
            String accidentDiseaseType,
            String title,
            String matchedChunk,
            double distance,
            double similarityScore
    ) {
    }
}