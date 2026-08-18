package egovframework.workcare.precedent.service;

import java.util.List;

/**
 * 의미 기반 판례 검색 결과다.
 *
 * @param matches 검색된 판례
 */
public record SemanticPrecedentSearchResult(
        List<Match> matches
) {

    /**
     * 질문과 관련된 판례 한 건이다.
     *
     * @param caseNumber          사건번호
     * @param courtName           법원명
     * @param resultType          판결결과
     * @param caseType            사건유형
     * @param accidentDiseaseType 사고·질병 구분
     * @param title               사건명
     * @param matchedChunk        질문과 가장 유사한 판결문 부분
     * @param distance            코사인 거리
     * @param similarityScore     사용자가 이해하기 쉬운 유사도 점수
     */
    public record Match(
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