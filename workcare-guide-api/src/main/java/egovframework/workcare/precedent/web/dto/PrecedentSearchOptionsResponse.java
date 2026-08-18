package egovframework.workcare.precedent.web.dto;

import java.util.List;

/**
 * 판례 검색 화면의 선택항목을 반환한다.
 */
public record PrecedentSearchOptionsResponse(
        List<String> resultTypes,
        List<String> caseTypes,
        List<String> accidentDiseaseTypes
) {
}