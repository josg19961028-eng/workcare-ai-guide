package egovframework.workcare.precedent.service;

import java.util.List;

/**
 * 판례 검색 업무의 Service 결과 모델이다.
 */
public record PrecedentList(
        int page,
        int size,
        int totalCount,
        List<Precedent> precedents
) {

    /**
     * 우리 서비스에서 사용하는 판례 한 건이다.
     */
    public record Precedent(
            String caseNumber,
            String courtName,
            String resultType,
            String caseType,
            String accidentDiseaseType,
            String title,
            String content
    ) {
    }
}